package bg.edu.utp.aitravelbenchmark.comparison;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComparisonService {

    private static final String CHATGPT = "ChatGPT";
    private static final String GEMINI = "Gemini";
    private static final String MICROSOFT = "Microsoft Phi";

    /*
     * При тези полета стойност 0 не представлява
     * реална финансова оценка и не носи точки.
     *
     * Не включваме полета като visaCost или otherCosts,
     * защото при тях нулата може да бъде напълно коректна.
     */
    private static final Set<String>
            REQUIRED_POSITIVE_MONEY_FIELDS = Set.of(
            "/budget/maximumBudgetPerPerson",
            "/budget/flightCost",
            "/budget/accommodationCost",
            "/budget/foodCost",
            "/budget/activitiesCost",
            "/budget/totalEstimatedCost",
            "/flight/estimatedTicketPrice",
            "/accommodation/pricePerNight",
            "/accommodation/totalAccommodationPrice"
    );

    public BenchmarkComparisonResponse compare(
            JsonNode chatGptResponse,
            JsonNode geminiResponse,
            JsonNode microsoftResponse
    ) {
        Map<String, JsonNode> modelResponses =
                new LinkedHashMap<>();

        modelResponses.put(
                CHATGPT,
                chatGptResponse
        );

        modelResponses.put(
                GEMINI,
                geminiResponse
        );

        modelResponses.put(
                MICROSOFT,
                microsoftResponse
        );

        List<TableDefinition> definitions =
                createTableDefinitions();

        Map<String, Double> totalScores =
                new LinkedHashMap<>();

        modelResponses
                .keySet()
                .forEach(model ->
                        totalScores.put(
                                model,
                                0.0
                        )
                );

        List<ComparisonTable> tables =
                new ArrayList<>();

        for (TableDefinition definition : definitions) {
            ComparisonTable table =
                    evaluateTable(
                            definition,
                            modelResponses
                    );

            tables.add(table);

            table.scores().forEach(
                    (model, score) ->
                            totalScores.merge(
                                    model,
                                    score,
                                    Double::sum
                            )
            );
        }

        List<RankingEntry> ranking =
                createRanking(totalScores);

        return new BenchmarkComparisonResponse(
                tables,
                totalScores,
                ranking
        );
    }

    private ComparisonTable evaluateTable(
            TableDefinition definition,
            Map<String, JsonNode> modelResponses
    ) {
        List<ComparisonRow> rows =
                new ArrayList<>();

        Map<String, Double> scores =
                new LinkedHashMap<>();

        modelResponses
                .keySet()
                .forEach(model ->
                        scores.put(
                                model,
                                0.0
                        )
                );

        for (FieldDefinition field : definition.fields()) {
            Map<String, String> values =
                    new LinkedHashMap<>();

            for (
                    Map.Entry<String, JsonNode> entry
                    : modelResponses.entrySet()
            ) {
                String model =
                        entry.getKey();

                JsonNode response =
                        entry.getValue();

                JsonNode valueNode =
                        response.at(
                                field.jsonPointer()
                        );

                String displayValue =
                        formatValue(valueNode);

                values.put(
                        model,
                        displayValue
                );

                if (
                        hasMeaningfulValue(
                                field,
                                valueNode
                        )
                ) {
                    scores.merge(
                            model,
                            field.points(),
                            Double::sum
                    );
                }
            }

            rows.add(
                    new ComparisonRow(
                            field.label(),
                            values
                    )
            );
        }

        String winner =
                findWinner(scores);

        return new ComparisonTable(
                definition.title(),
                rows,
                scores,
                winner
        );
    }

    /**
     * Проверява дали дадена стойност трябва да получи точки.
     */
    private boolean hasMeaningfulValue(
            FieldDefinition field,
            JsonNode node
    ) {
        if (
                node == null
                        || node.isMissingNode()
                        || node.isNull()
        ) {
            return false;
        }

        if (node.isTextual()) {
            return !node
                    .asText()
                    .isBlank();
        }

        if (node.isArray() || node.isObject()) {
            return !node.isEmpty();
        }

        /*
         * При задължителните парични полета стойността
         * трябва да бъде реално число, по-голямо от нула.
         */
        if (
                isRequiredPositiveMoneyField(
                        field.jsonPointer()
                )
        ) {
            return isPositiveNumber(node);
        }

        /*
         * Boolean false остава валидна стойност.
         *
         * Например:
         * withinBudget = false
         * visaRequired = false
         * breakfastIncluded = false
         *
         * Това са реални отговори, а не липсващи данни.
         */
        if (node.isBoolean()) {
            return true;
        }

        /*
         * При останалите числови полета нулата засега
         * се приема за валидна, защото може да е логична.
         *
         * Например:
         * numberOfLayovers = 0
         */
        if (node.isNumber()) {
            return true;
        }

        return true;
    }

    private boolean isRequiredPositiveMoneyField(
            String jsonPointer
    ) {
        return jsonPointer != null
                && REQUIRED_POSITIVE_MONEY_FIELDS.contains(
                jsonPointer
        );
    }

    private boolean isPositiveNumber(
            JsonNode node
    ) {
        return node != null
                && node.isNumber()
                && Double.isFinite(
                node.asDouble()
        )
                && node.asDouble() > 0;
    }

    private String formatValue(
            JsonNode node
    ) {
        if (
                node == null
                        || node.isMissingNode()
                        || node.isNull()
        ) {
            return "Not provided";
        }

        if (node.isTextual()) {
            return node
                    .asText()
                    .isBlank()
                    ? "Not provided"
                    : node.asText();
        }

        if (node.isArray()) {
            if (node.isEmpty()) {
                return "Not provided";
            }

            List<String> values =
                    new ArrayList<>();

            node.forEach(item -> {
                if (item.isValueNode()) {
                    values.add(
                            item.asText()
                    );
                } else {
                    values.add(
                            item.toString()
                    );
                }
            });

            return String.join(
                    ", ",
                    values
            );
        }

        return node.toString();
    }

    private String findWinner(
            Map<String, Double> scores
    ) {
        double maximum =
                scores.values()
                        .stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .max()
                        .orElse(0);

        return scores.entrySet()
                .stream()
                .filter(entry ->
                        Double.compare(
                                entry.getValue(),
                                maximum
                        ) == 0
                )
                .map(
                        Map.Entry::getKey
                )
                .collect(
                        Collectors.joining(", ")
                );
    }

    private List<RankingEntry> createRanking(
            Map<String, Double> totalScores
    ) {
        List<Map.Entry<String, Double>> sorted =
                totalScores.entrySet()
                        .stream()
                        .sorted(
                                Map.Entry
                                        .<String, Double>
                                                comparingByValue()
                                        .reversed()
                        )
                        .toList();

        List<RankingEntry> ranking =
                new ArrayList<>();

        for (
                int index = 0;
                index < sorted.size();
                index++
        ) {
            Map.Entry<String, Double> entry =
                    sorted.get(index);

            ranking.add(
                    new RankingEntry(
                            index + 1,
                            entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        return ranking;
    }

    private List<TableDefinition>
    createTableDefinitions() {
        return List.of(
                new TableDefinition(
                        "Trip Summary",
                        List.of(
                                new FieldDefinition(
                                        "Origin",
                                        "/tripSummary/origin",
                                        1
                                ),
                                new FieldDefinition(
                                        "Destination",
                                        "/tripSummary/destination",
                                        1
                                ),
                                new FieldDefinition(
                                        "Start date",
                                        "/tripSummary/startDate",
                                        1
                                ),
                                new FieldDefinition(
                                        "End date",
                                        "/tripSummary/endDate",
                                        1
                                ),
                                new FieldDefinition(
                                        "Number of days",
                                        "/tripSummary/numberOfDays",
                                        1
                                ),
                                new FieldDefinition(
                                        "Number of nights",
                                        "/tripSummary/numberOfNights",
                                        1
                                )
                        )
                ),

                new TableDefinition(
                        "Budget Comparison",
                        List.of(
                                new FieldDefinition(
                                        "Maximum budget per person",
                                        "/budget/maximumBudgetPerPerson",
                                        1
                                ),
                                new FieldDefinition(
                                        "Flight cost",
                                        "/budget/flightCost",
                                        1
                                ),
                                new FieldDefinition(
                                        "Accommodation cost",
                                        "/budget/accommodationCost",
                                        1
                                ),
                                new FieldDefinition(
                                        "Food cost",
                                        "/budget/foodCost",
                                        1
                                ),
                                new FieldDefinition(
                                        "Activities cost",
                                        "/budget/activitiesCost",
                                        1
                                ),
                                new FieldDefinition(
                                        "Total estimated cost",
                                        "/budget/totalEstimatedCost",
                                        2
                                ),
                                new FieldDefinition(
                                        "Within budget",
                                        "/budget/withinBudget",
                                        2
                                )
                        )
                ),

                new TableDefinition(
                        "Flight Comparison",
                        List.of(
                                new FieldDefinition(
                                        "Airline company",
                                        "/flight/airlineCompany",
                                        1
                                ),
                                new FieldDefinition(
                                        "Flight number",
                                        "/flight/flightNumber",
                                        1
                                ),
                                new FieldDefinition(
                                        "Flight class",
                                        "/flight/flightClass",
                                        1
                                ),
                                new FieldDefinition(
                                        "Departure airport",
                                        "/flight/departureAirport",
                                        1
                                ),
                                new FieldDefinition(
                                        "Arrival airport",
                                        "/flight/arrivalAirport",
                                        1
                                ),
                                new FieldDefinition(
                                        "Departure time",
                                        "/flight/departureTime",
                                        1
                                ),
                                new FieldDefinition(
                                        "Arrival time",
                                        "/flight/arrivalTime",
                                        1
                                ),
                                new FieldDefinition(
                                        "Flight duration",
                                        "/flight/flightDurationMinutes",
                                        1
                                ),
                                new FieldDefinition(
                                        "Baggage details",
                                        "/flight/baggageDetails",
                                        1
                                )
                        )
                ),

                new TableDefinition(
                        "Accommodation Comparison",
                        List.of(
                                new FieldDefinition(
                                        "Hotel name",
                                        "/accommodation/hotelName",
                                        1
                                ),
                                new FieldDefinition(
                                        "Hotel category",
                                        "/accommodation/hotelCategoryStars",
                                        1
                                ),
                                new FieldDefinition(
                                        "Address",
                                        "/accommodation/address",
                                        1
                                ),
                                new FieldDefinition(
                                        "Guest rating",
                                        "/accommodation/guestRating",
                                        1
                                ),
                                new FieldDefinition(
                                        "Breakfast included",
                                        "/accommodation/breakfastIncluded",
                                        1
                                ),
                                new FieldDefinition(
                                        "Price per night",
                                        "/accommodation/pricePerNight",
                                        1
                                ),
                                new FieldDefinition(
                                        "Total accommodation price",
                                        "/accommodation/totalAccommodationPrice",
                                        1
                                ),
                                new FieldDefinition(
                                        "Cancellation conditions",
                                        "/accommodation/cancellationConditions",
                                        1
                                )
                        )
                ),

                new TableDefinition(
                        "Legal and Operational Information",
                        List.of(
                                new FieldDefinition(
                                        "Visa information",
                                        "/legalOperationalInformation/visaType",
                                        1
                                ),
                                new FieldDefinition(
                                        "Passport validity",
                                        "/legalOperationalInformation/passportValidityRequirements",
                                        1
                                ),
                                new FieldDefinition(
                                        "Travel insurance",
                                        "/legalOperationalInformation/travelInsuranceRequired",
                                        1
                                ),
                                new FieldDefinition(
                                        "Entry requirements",
                                        "/legalOperationalInformation/entryRequirements",
                                        1
                                ),
                                new FieldDefinition(
                                        "Health requirements",
                                        "/legalOperationalInformation/healthRequirements",
                                        1
                                ),
                                new FieldDefinition(
                                        "Cancellation conditions",
                                        "/legalOperationalInformation/cancellationConditions",
                                        1
                                ),
                                new FieldDefinition(
                                        "Emergency number",
                                        "/legalOperationalInformation/emergencyNumber",
                                        1
                                )
                        )
                ),

                new TableDefinition(
                        "Services",
                        List.of(
                                new FieldDefinition(
                                        "Included services",
                                        "/includedServices",
                                        3
                                ),
                                new FieldDefinition(
                                        "Excluded services",
                                        "/excludedServices",
                                        3
                                ),
                                new FieldDefinition(
                                        "Optional tours",
                                        "/optionalTours",
                                        2
                                )
                        )
                ),

                new TableDefinition(
                        "Additional Information",
                        List.of(
                                new FieldDefinition(
                                        "Tourist attractions",
                                        "/touristAttractions",
                                        2
                                ),
                                new FieldDefinition(
                                        "Daily tour program",
                                        "/dailyTourProgram",
                                        3
                                ),
                                new FieldDefinition(
                                        "Restaurants",
                                        "/foodAndRestaurants/restaurants",
                                        1
                                ),
                                new FieldDefinition(
                                        "Safety warnings",
                                        "/safety/touristWarnings",
                                        1
                                ),
                                new FieldDefinition(
                                        "Sources",
                                        "/sources",
                                        2
                                ),
                                new FieldDefinition(
                                        "Assumptions",
                                        "/selfAssessment/assumptions",
                                        1
                                )
                        )
                )
        );
    }
}