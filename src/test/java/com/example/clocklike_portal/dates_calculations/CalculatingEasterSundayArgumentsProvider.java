package com.example.clocklike_portal.dates_calculations;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.time.LocalDate;
import java.util.stream.Stream;

public class CalculatingEasterSundayArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                // year
                // expected date
                Arguments.of(
                        2023,
                        LocalDate.of(2023, 4, 9)
                ),
                Arguments.of(
                        2024,
                        LocalDate.of(2024, 3, 31)
                ),
                Arguments.of(
                        2025,
                        LocalDate.of(2025, 4, 20)
                )
        );
    }
}
