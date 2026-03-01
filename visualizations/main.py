#!/bin/python3
import pandas as pd
import plotly.express as px

# chosen by agencies with more than 100 deployed entries in 2025 (rearranged)
significant_agencies = [
    "Department of Homeland Security",
    "Department of Justice",
    "Department of Health and Human Services",
    "Department of Veterans Affairs",
    "Department of Energy",
]

significant_dhs_bureaus = [
    "Immigration and Customs Enforcement",
    "Customs and Border Protection",
    "United States Citizenship and Immigration Services",
    "Transportation Security Administration",
]

# chosen by agencies with more than 10 high-impact deployed entries in 2025 (rearranged)
significant_agencies_high_impact = [
    "Department of Homeland Security",
    "Department of Justice",
    "Department of Veterans Affairs",
]

stage_of_development_replacements = {
    None: "Unknown",

    r"^OPC.*": "Pre-deployment",
    r"^Research.*": "Pre-deployment",
    r"^Technical transfer.*": "Pre-deployment",
    r".*Proof[- ]of[- ][Cc]oncept.*": "Pre-deployment",
    r".*[Dd]evelopment.*": "Pre-deployment",
    r"^Initiat(ed|ion)": "Pre-deployment",
    r"^Planned.*": "Pre-deployment",
    r".*Pre[- ][Dd]eployment.*": "Pre-deployment",

    r"^User Acceptance Testing.*": "Pilot",
    "Implementation and Assessment": "Pilot",
    "This is a pilot initiative": "Pilot",
    "Refinments planned for future release": "Pilot",
    "Successfully tested but not in production.": "Pilot",

    "In-use": "Deployed",
    "In mission": "Deployed",
    "Implementation": "Deployed",
    "Deployment": "Deployed",
    r"^Operation.*": "Deployed",
    r"^In production.*": "Deployed",

    "Completed": "Retired",
}

agency_replacements = {
    r"\sOf\s": " of ",
    r"\sThe\s": " the ",
    r"\sAnd\s": " and ",
}

bureau_replacements = {
    r"HQ.*": "Department-wide",
    "DHS": "Department-wide",
    "CBP": "Customs and Border Protection",
    "CISA": "Cybersecurity and Infrastructure Security Agency",
    "FEMA": "Federal Emergency Management Agency",
    "ICE": "Immigration and Customs Enforcement",
    "MGMT": "Management Directorate",
    "TSA": "Transportation Security Administration",
    "USCG": "United States Coast Guard",
    "USCIS": "United States Citizenship and Immigration Services",
    "USSS": "United States Secret Service",
}

def standardize_data(df, year):
    if year == 2025:
        df = df.rename(columns={
            "Agency": "agency",
            "Bureau/Component": "bureau",
            "Stage of Development": "stage",
            "Is the AI use case high-impact?": "impact"
        })

    elif year == 2024:
        df = df.rename(columns={
            "3_agency": "agency",
            "4_bureau": "bureau",
            "16_dev_stage": "stage",
            "17_impact_type": "impact"
        })

    elif year == 2023:
        df = df.rename(columns={
            "Department": "agency",
            "Agency": "bureau",
            "Development_Stage": "stage"
        })
        df["impact"] = None  # no impact field in 2023

    df["stage"] = df["stage"].replace(stage_of_development_replacements, regex=True)
    df["agency"] = df["agency"].replace(agency_replacements, regex=True)
    df["bureau"] = df["bureau"].replace(bureau_replacements, regex=True)

    df["agency"] = df["agency"].str.strip()
    df["impact"] = df["impact"].str.strip()

    df["year"] = year
    return df

def main():
    df_2025 = pd.read_csv("../inventory/data/clean/2025_consolidated_ai_inventory.csv")
    df_2024 = pd.read_csv("../inventory/data/clean/2024_consolidated_ai_inventory_raw.csv")
    df_2023 = pd.read_csv("../inventory/data/clean/2023_consolidated_ai_inventory_raw.csv")

    df_2025 = standardize_data(df_2025, 2025)
    df_2024 = standardize_data(df_2024, 2024)
    df_2023 = standardize_data(df_2023, 2023)

    df_all = pd.concat([df_2023, df_2024, df_2025], ignore_index=True)

    fig = px.histogram(df_all, x="year", color="stage",
                       title="Stage of Development of All AI Use Cases",
                       category_orders={"stage": ["Deployed", "Pre-deployment", "Pilot", "Retired", "Unknown"]})
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.show(renderer="browser")

    df_deployed = df_all[df_all["stage"]=="Deployed"]
    df_deployed.loc[~df_deployed["agency"].isin(significant_agencies), "agency"] = "Other"

    fig = px.histogram(df_deployed, x="year", color="agency",
                       title="Agency of Deployed AI Use Cases",
                       category_orders={"agency": significant_agencies})
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.show(renderer="browser")

    df_dhs_deployed = df_deployed[df_deployed["agency"]=="Department of Homeland Security"]
    df_dhs_deployed.loc[~df_dhs_deployed["bureau"].isin(significant_dhs_bureaus), "bureau"] = "Other"

    fig = px.histogram(df_dhs_deployed, x="year", color="bureau",
                       title="Bureau of DHS Deployed AI Use Cases",
                       category_orders={"bureau": significant_dhs_bureaus})
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.show(renderer="browser")

    df_deployed_high_impact = df_deployed[df_deployed["impact"].isin({"High-impact", "Rights-Impacting", "Safety-Impacting", "Both"})]
    df_deployed_high_impact.loc[~df_deployed_high_impact["agency"].isin(significant_agencies_high_impact), "agency"] = "Other"

    fig = px.histogram(df_deployed_high_impact, x="year", color="agency",
                       title="Agency of High-Impact Deployed AI Use Cases",
                       category_orders={"agency": significant_agencies_high_impact})
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.show(renderer="browser")

    df_dhs_deployed_high_impact = df_deployed_high_impact[df_deployed_high_impact["agency"]=="Department of Homeland Security"]
    df_dhs_deployed_high_impact.loc[~df_dhs_deployed_high_impact["bureau"].isin(significant_dhs_bureaus), "bureau"] = "Other"

    fig = px.histogram(df_dhs_deployed_high_impact, x="year", color="bureau",
                       title="Bureau of DHS High-Impact Deployed AI Use Cases",
                       category_orders={"bureau": significant_dhs_bureaus})
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.show(renderer="browser")

if __name__ == "__main__":
    main()