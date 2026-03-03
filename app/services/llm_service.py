import json
import logging

from openai import AsyncOpenAI

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# PII Classification categories with descriptions and examples
PII_CATEGORIES = {
    "Email Address": {
        "description": "Personal and business email addresses",
        "examples": ["john.doe@gmail.com", "user@company.com", "ahmet.yilmaz@email.com"],
    },
    "Phone Number": {
        "description": "Mobile, landline, and international phone numbers",
        "examples": ["+90 532 123 4567", "+1-555-123-4567", "(555) 123-4567"],
    },
    "Social Security Number (SSN)": {
        "description": "US Social Security Numbers and similar national identifiers",
        "examples": ["123-45-6789", "987654321"],
    },
    "Credit Card Number": {
        "description": "Payment card numbers (Visa, MasterCard, etc.), including masked formats",
        "examples": ["4532-1234-5678-9012", "5555555555554444", "****-****-****-1234"],
    },
    "National ID Number": {
        "description": "Government-issued identification numbers such as passport numbers, driver's license numbers",
        "examples": ["AB1234567", "DL-9876543"],
    },
    "Full Name": {
        "description": "Complete personal names including first, middle, and last name together",
        "examples": ["John Michael Smith", "Maria Garcia Lopez", "Ahmet Yılmaz"],
    },
    "First Name": {
        "description": "Personal first names only (without surname)",
        "examples": ["John", "Maria", "Ahmet", "Fatma"],
    },
    "Last Name / Surname": {
        "description": "Family names or surnames only (without first name)",
        "examples": ["Smith", "Johnson", "Yılmaz", "Demir"],
    },
    "TCKN (Turkish Citizenship Number)": {
        "description": "Turkish Republic Citizenship Number - an 11-digit unique identifier for Turkish citizens",
        "examples": ["12345678901", "98765432109"],
    },
    "Home Address": {
        "description": "Residential addresses including street, city, state, ZIP or postal code",
        "examples": ["123 Main St, Anytown, CA 90210", "Atatürk Caddesi No:123, Kadıköy, İstanbul"],
    },
    "Date of Birth": {
        "description": "Personal birth dates in various formats",
        "examples": ["1985-03-15", "March 15, 1985", "15/03/1985"],
    },
    "IP Address": {
        "description": "Internet Protocol addresses (IPv4 and IPv6) that can identify individuals",
        "examples": ["192.168.1.1", "10.0.0.50", "2001:0db8:85a3::8a2e:0370:7334"],
    },
    "Not PII": {
        "description": "Data that does not contain any personally identifiable information",
        "examples": ["product_name", "order_status", "unit_price", "stock_quantity"],
    },
}

CATEGORY_NAMES = list(PII_CATEGORIES.keys())


def get_llm_client() -> AsyncOpenAI:
    """Create an OpenAI-compatible async client."""
    return AsyncOpenAI(
        api_key=settings.OPENAI_API_KEY.get_secret_value(),
        base_url=settings.OPENAI_BASE_URL,
    )


def build_classification_prompt(
    column_name: str,
    table_name: str,
    data_type: str,
    samples: list[str],
) -> str:
    """Build a detailed, structured prompt for PII classification."""

    # Build category descriptions with examples
    categories_section = ""
    for i, (cat_name, cat_info) in enumerate(PII_CATEGORIES.items(), 1):
        examples_str = ", ".join([f'"{ex}"' for ex in cat_info["examples"]])
        categories_section += (
            f"  {i}. **{cat_name}**\n"
            f"     Description: {cat_info['description']}\n"
            f"     Examples: {examples_str}\n\n"
        )

    samples_text = "\n".join([f"  {i+1}. {s}" for i, s in enumerate(samples)])

    prompt = f"""You are an expert data privacy analyst specializing in PII (Personally Identifiable Information) detection and classification in databases. Your task is to analyze a database column and determine what type of data it contains.

## Context

You are analyzing a PostgreSQL database that may contain data in multiple languages, including **Turkish** and **English**. Pay special attention to Turkish-specific data formats such as:
- Turkish names (e.g., Ahmet, Fatma, Yılmaz, Demir)
- Turkish phone numbers (e.g., +90 5XX XXX XXXX)
- Turkish addresses (e.g., Caddesi, Mahallesi, Sokak)
- TCKN format (exactly 11 digits, Turkish national ID)

## Column Under Analysis

| Property     | Value            |
|------------- |------------------|
| Column Name  | `{column_name}`  |
| Table Name   | `{table_name}`   |
| Data Type    | `{data_type}`    |

## Sample Data ({len(samples)} samples)

{samples_text}

## PII Categories

Classify the data into one or more of the following categories:

{categories_section}

## Classification Rules

1. **Analyze holistically**: Consider the column name, table name, SQL data type, AND the actual sample values together. Do not rely on just one signal.
2. **Primary classification**: Assign the highest probability to the single most likely category. This should typically be >= 0.80 if you are confident.
3. **Secondary signals**: If data could belong to multiple categories (e.g., a name field could be "First Name" or "Full Name"), distribute probability accordingly.
4. **Sum constraint**: All probabilities MUST sum to exactly 1.0.
5. **Not PII default**: If the data clearly does not contain any personal information (e.g., product names, prices, statuses, dates that are not birth dates), assign high probability (>= 0.90) to "Not PII".
6. **Masked/partial data**: If data appears masked (e.g., "****-****-****-1234"), still classify based on the pattern and column name.
7. **Edge cases**:
   - UUID/auto-generated IDs → "Not PII"
   - Timestamps that are not birth dates (e.g., created_at, updated_at) → "Not PII"
   - Boolean fields, status fields, quantity fields → "Not PII"

## Few-Shot Examples

**Example 1**: Column `email` in table `users`, type `varchar`, samples: ["john@gmail.com", "jane@company.com"]
→ Email Address: 0.97, Not PII: 0.02, Full Name: 0.01

**Example 2**: Column `stock_quantity` in table `products`, type `integer`, samples: ["15", "25", "100"]
→ Not PII: 0.99, Phone Number: 0.01

**Example 3**: Column `first_name` in table `employees`, type `varchar`, samples: ["Ali", "Zeynep", "Burak"]
→ First Name: 0.92, Full Name: 0.05, Last Name / Surname: 0.02, Not PII: 0.01

## Required Output Format

Return a JSON object with a "classifications" key containing an array of objects. Each object must have "category" (string) and "probability" (float) fields.

{{
  "classifications": [
    {{"category": "Email Address", "probability": 0.95}},
    {{"category": "Not PII", "probability": 0.03}},
    {{"category": "Phone Number", "probability": 0.02}}
  ]
}}

IMPORTANT: Return ONLY the JSON object. No explanation, no markdown, no additional text."""

    return prompt


async def classify_with_llm(
    column_name: str,
    table_name: str,
    data_type: str,
    samples: list[str],
) -> list[dict]:
    """
    Send column samples to LLM for PII classification.
    Returns probability distribution across all PII categories.
    """
    client = get_llm_client()

    prompt = build_classification_prompt(column_name, table_name, data_type, samples)

    try:
        response = await client.chat.completions.create(
            model=settings.OPENAI_MODEL,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a data privacy and PII classification expert. "
                        "You analyze database columns and classify them into PII categories. "
                        "You always respond with valid JSON only, no markdown formatting, no explanations."
                    ),
                },
                {"role": "user", "content": prompt},
            ],
            temperature=0.1,
            max_tokens=1500,
            response_format={"type": "json_object"},
        )

        content = response.choices[0].message.content.strip()

        # Clean up response - remove markdown code blocks if present
        if content.startswith("```"):
            content = content.split("\n", 1)[1]
            if content.endswith("```"):
                content = content.rsplit("```", 1)[0]
            content = content.strip()

        parsed = json.loads(content)

        # Handle both formats: direct array or {"classifications": [...]}
        if isinstance(parsed, list):
            classifications = parsed
        elif isinstance(parsed, dict) and "classifications" in parsed:
            classifications = parsed["classifications"]
        else:
            # Try to find any array in the response
            for value in parsed.values():
                if isinstance(value, list):
                    classifications = value
                    break
            else:
                raise ValueError("Could not find classifications array in LLM response")

        # Validate and normalize
        result = []
        for item in classifications:
            category = item.get("category", "")
            probability = float(item.get("probability", 0.0))
            probability = max(0.0, min(1.0, probability))

            if category in CATEGORY_NAMES:
                result.append({"category": category, "probability": round(probability, 4)})

        # Ensure all categories are present
        existing_categories = {r["category"] for r in result}
        for cat in CATEGORY_NAMES:
            if cat not in existing_categories:
                result.append({"category": cat, "probability": 0.0})

        # Normalize probabilities to sum to 1.0
        total = sum(r["probability"] for r in result)
        if total > 0:
            for r in result:
                r["probability"] = round(r["probability"] / total, 4)

        # Sort by probability descending
        result.sort(key=lambda x: x["probability"], reverse=True)

        return result

    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse LLM response as JSON: {e}")
        raise ValueError(f"LLM returned invalid JSON response: {e}")
    except Exception as e:
        logger.error(f"LLM classification failed: {e}")
        raise
