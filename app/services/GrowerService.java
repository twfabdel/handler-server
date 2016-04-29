package services;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import models.Grower;

import play.mvc.Controller;
import play.mvc.Result;

import services.parsers.GrowerJsonParser;



// TODO Figure out how to construct Result without needing to extend Controller.
public class GrowerService extends Controller {

  public static List<Grower> getAllGrowers() {
    return Grower.find.all();
  }

  public static Grower getGrower(Long id) {
    return Grower.find.byId(id);
  }

  public static Result createGrowerResult(JsonNode data) {
    GrowerJsonParser parser = new GrowerJsonParser(data);

    if (!parser.isValid()) {
      return badRequest(parser.getErrorMessage());
    }

    Grower grower = createGrower(parser);

    return created("Grower Created: " + grower + "\n");
  }

  private static Grower createGrower(GrowerJsonParser parser) {
    Grower newGrower = new Grower(
        parser.getHandler(),
        parser.getFirstName(),
        parser.getLastName(),
        parser.getEmailAddresses(),
        parser.getPhoneNumbers());

    newGrower.save();
    return newGrower;
  }
}
