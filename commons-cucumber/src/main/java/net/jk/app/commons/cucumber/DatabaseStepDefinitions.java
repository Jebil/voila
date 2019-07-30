package net.jk.app.commons.cucumber;

import io.cucumber.java.en.Given;
import java.sql.Connection;
import java.sql.SQLException;

/*
 * Cucumber DB steps
 */
public class DatabaseStepDefinitions {

  @Given("^I execute \"([^\"]*)\" SQL statement$")
  public void i_execute_SQL_statement(String dbId, String sql) throws SQLException {
    Connection connection = CucumberEnvironment.getDatabaseConnection(dbId);
    connection.prepareStatement(sql).execute();
  }
}
