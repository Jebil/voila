package net.jk.app.testserver.integTest;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    plugin = {"pretty:build/reports/cucumber/cucumber.txt"},
    strict = true,
    monochrome = true,
    glue = {"net.jk.app.commons.cucumber", "net.jk.app.testserver.integTest.cucumber"},
    tags = {
      "not @broken"
    } // temporarily put in a tag if you just want to run a single BDD while in dev
    )
public class TestServerCucumberRunner {}
