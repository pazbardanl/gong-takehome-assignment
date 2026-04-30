package io.gong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.gong.cli.CalendarAvailabilityCli;
import io.gong.repository.CalendarRepository;
import io.gong.service.AvailabilityCalculationService;
import io.gong.service.CalendarDataProvider;

@Configuration
@PropertySource(value = "classpath:io/gong/workday.properties", encoding = "UTF-8")
@ComponentScan(basePackages = {"io.gong.repository.impl", "io.gong.service.impl"})
public class CalendarApplicationConfiguration {

    @Bean
    public CalendarAvailabilityCli calendarAvailabilityCli(
            CalendarRepository calendarRepository,
            CalendarDataProvider dataProvider,
            AvailabilityCalculationService availabilityCalculationService) {
        return new CalendarAvailabilityCli(calendarRepository, dataProvider, availabilityCalculationService);
    }
}
