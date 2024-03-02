package iq.earthlink.social.personservice.controller.rest.person.v1;

import io.swagger.annotations.*;
import iq.earthlink.social.personservice.person.rest.JsonCity;
import iq.earthlink.social.personservice.service.CityService;
import org.dozer.Mapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "City Api", value = "CityApi")
@RestController
@RequestMapping(value = "/api/v1/cities", produces = MediaType.APPLICATION_JSON_VALUE)
public class CityApi {

  private final CityService cityService;
  private final Mapper mapper;

  public CityApi(CityService cityService, Mapper mapper) {
    this.cityService = cityService;
    this.mapper = mapper;
  }

  @GetMapping
  @ApiOperation("Returns list of the cities")
  @ApiResponses(
      @ApiResponse(message = "Array of city info items", code = 200, response = JsonCity[].class)
  )
  public List<JsonCity> getCities(
      @RequestParam(value = "query", required = false)
      @ApiParam("Text that should contain all cities in result list")
      String query
  ) {
    String locale =LocaleContextHolder.getLocale().getLanguage();
    return cityService.getCities(locale, query)
        .stream()
        .map(c -> mapper.map(c, JsonCity.class))
        .toList();
  }
}
