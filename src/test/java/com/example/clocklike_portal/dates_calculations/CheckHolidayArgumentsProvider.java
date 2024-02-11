package com.example.clocklike_portal.dates_calculations;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.time.LocalDate;
import java.util.stream.Stream;

public class CheckHolidayArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                // checked date
                // expected result
                Arguments.of(
                        LocalDate.of(2024, 1, 1),
                        true
                ),
                Arguments.of(
                        LocalDate.of(2025, 1, 1),
                        true
                ),
                Arguments.of(
                        LocalDate.of(2024, 5, 30),
                        true
                ),
                Arguments.of(
                        LocalDate.of(2023, 5, 30),
                        false
                )

        );
    }
}
