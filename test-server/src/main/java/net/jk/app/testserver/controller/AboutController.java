package net.jk.app.testserver.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Value;
import net.jk.app.commons.boot.security.UrlConstants;

@RestController
@RequestMapping(value = UrlConstants.PUBLIC + "/about", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(value = "/about", description = "Operations to get information about the service.")
public class AboutController {
	private final Optional<BuildProperties> buildProperties;

	public AboutController(@Autowired(required = false) BuildProperties buildProperties) {
		this.buildProperties = Optional.ofNullable(buildProperties);
	}

	@GetMapping("/version")
	@ApiOperation(value = "Get the build version.", notes = "Returns the build version.")
	@Timed
	public BuildVersion getBuildVersion() {
		return new BuildVersion(buildProperties.map(bp -> bp.get("app.version")).orElse(null));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Value
	public static class BuildVersion {

		private final String version;

		public String getVersion() {
			return version != null ? version : "DEV";
		}
	}
}
