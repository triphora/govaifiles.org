#!/bin/python3
import pandas as pd
import plotly.express as px
from dash import Dash, dcc, html, Output, Input

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
    "Ideation": "Pre-deployment",

    r"^User Acceptance Testing.*": "Pilot",
    "Implementation and Assessment": "Pilot",
    "This is a pilot initiative": "Pilot",
    # misspelling intentional (as is in dataset)
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

stage_of_development_order = ["Deployed", "Pre-deployment", "Pilot", "Retired", "Unknown"]
impact_order = ["High-impact", "Not high-impact"]

impact_replacements = {
    r"^Rights-[Ii]mpacting.?": "High-impact",
    r"^Safety-[Ii]mpacting.?": "High-impact",
    "Both": "High-impact",

    "Neither": "Not high-impact",
    "Case-by-case assessment": "Not high-impact",
    r"^No.*": "Not high-impact",
}

agency_replacements = {
    r"\sOf\s": " of ",
    r"\sThe\s": " the ",
    r"\sAnd\s": " and ",
    r"^U.S.": "",
    r"^United States": "",
}

bureau_replacements = {
    r"HQ.*": "Department-wide",
    "DHS": "Department-wide",
    "CBP": "Customs and Border Protection",
    "CISA": "Cybersecurity and Infrastructure Security Agency",
    "CWMD": "Countering Weapons of Mass Destruction",
    "FEMA": "Federal Emergency Management Agency",
    "ICE": "Immigration and Customs Enforcement",
    "MGMT": "Management Directorate",
    "OHS": "Office of Health Security",
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
            "Agency": "agency",
            "Bureau": "bureau",
            "Stage of Development": "stage",
            "Is the AI use case rights-impacting, safety-impacting, both, or neither?": "impact"
        })

    elif year == 2023:
        df = df.rename(columns={
            "Department": "agency",
            "Agency": "bureau",
            "Development_Stage": "stage"
        })
        df["impact"] = None  # no impact field in 2023

    df["stage"] = df["stage"].replace(stage_of_development_replacements, regex=True)
    df["impact"] = df["impact"].replace(impact_replacements, regex=True)
    df["agency"] = df["agency"].replace(agency_replacements, regex=True)
    df["bureau"] = df["bureau"].replace(bureau_replacements, regex=True)

    df["agency"] = df["agency"].str.strip()
    df["impact"] = df["impact"].str.strip()

    df["year"] = year
    return df

df_2025 = pd.read_csv("../inventory/data/clean/2025_consolidated_ai_inventory.csv")
df_2024 = pd.read_csv("../inventory/data/clean/2024_consolidated_ai_inventory_raw_v2.csv", encoding="ISO-8859-15")
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

agency_order = sorted(df_all.agency.unique(), key=lambda s: (not s.startswith("Department of"), s))

app = Dash(
    __name__,
    requests_pathname_prefix="/plotly/",
    routes_pathname_prefix="/plotly/",
)

app.layout = html.Div([
    dcc.Graph(id="histogram"),
    dcc.Dropdown(
        id="color-dropdown",
        options=[{"label": "Group by Stage of Development", "value": "stage"}] +
                [{"label": "Group by Impact", "value": "impact"}] +
                [{"label": "Group by Agency", "value": "agency"}],
        value="stage",
        clearable=False,
    ),
    html.H3(
        "Filters",
        style={"margin-bottom": "0.25rem"}
    ),
    html.Div(
        style={
            "display": "flex",
            "gap": "8px",
            "flexWrap": "wrap",
        },
        children=[
            dcc.Dropdown(
                id="stage-dropdown",
                options=[{"label": "Any Stage of Development", "value": "Any"}] +
                        [{"label": s, "value": s} for s in stage_of_development_order],
                value="Any",
                style={"width": "235px"},
                clearable=False,
            ),
            dcc.Dropdown(
                id="impact-dropdown",
                options=[{"label": "Any Impact", "value": "Any"}] +
                        [{"label": s, "value": s} for s in impact_order],
                value="Any",
                style={"width": "165px"},
                clearable=False,
            ),
            dcc.Dropdown(
                id="agency-dropdown",
                options=[{"label": "Any Agency", "value": "Any Agency"}] +
                        [{"label": "Any Cabinet Agency", "value": "Any Cabinet Agency"}] +
                        [{"label": s, "value": s} for s in agency_order],
                value="Any Agency",
                style={"width": "365px"},
                clearable=False,
            ),
        ]
    ),
    html.Div(
        id="dhs-bureau-dropdown-container",
        children=[
            dcc.Dropdown(
                id="dhs-bureau-dropdown",
                options=[{"label": "Any DHS Bureau", "value": "Any"}] +
                        [{"label": s, "value": s} for s in sorted(df_all[df_all.agency=="Department of Homeland Security"].bureau.unique())],
                value="Any",
                style={"margin-top": "0.5em"},
                clearable=False,
            )
        ],
        style={"display": "none"}
    )
])
@app.callback(
    Output("histogram", "figure"),
    Input("stage-dropdown", "value"),
    Input("impact-dropdown", "value"),
    Input("agency-dropdown", "value"),
    Input("dhs-bureau-dropdown", "value"),
    Input("color-dropdown", "value"),
)
def update(stage, impact, agency, dhs_bureau, color):
    dff = df_all if stage == "Any" else df_all[df_all.stage == stage]
    dff = dff if impact == "Any" else dff[dff.impact == impact]
    match agency:
        case "Any Agency":
            pass
        case "Any Cabinet Agency":
            dff = dff[dff.agency.str.startswith("Department of")]
        case _:
            dff = dff[dff.agency == agency]
    if dhs_bureau != "Any":
        dff = dff[dff.bureau == dhs_bureau]

    category_orders = {"stage": stage_of_development_order}
    match color:
        case "impact":
            category_orders = {"impact": impact_order}
        case "agency":
            dff.loc[~dff["agency"].isin(significant_agencies), "agency"] = "Other"
            dff.loc[~dff["bureau"].isin(significant_dhs_bureaus), "bureau"] = "Other"
            if agency == "Department of Homeland Security":
                color = "bureau"
                category_orders = {"bureau": significant_dhs_bureaus}
            else:
                category_orders = {"agency": significant_agencies}

    fig = px.histogram(dff, x="year", color=color,
                       #title="AI Use Cases",
                       category_orders=category_orders)
    fig.update_layout(bargap=0.1)
    fig.update_xaxes(dtick=1)
    fig.update_layout(
        legend=dict(
            orientation="h",
            yanchor="bottom",
            y=1.02,
            xanchor="left",
            x=0
        )
    )

    return fig

@app.callback(
    Output("dhs-bureau-dropdown-container", "style"),
    Input("agency-dropdown", "value"),
)
def toggle_dropdown(val):
    return {"display": "block" if val == "Department of Homeland Security" else "none"}

server = app.server
