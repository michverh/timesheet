package eu.openvalue.timesheet.adapter.out.spreadsheet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;

/**
 * @param template the company's Excel template to fill in
 */
@ConfigurationProperties("timesheet.spreadsheet")
public record SpreadsheetProperties(@DefaultValue("classpath:timesheet.xlsx") Resource template) {
}
