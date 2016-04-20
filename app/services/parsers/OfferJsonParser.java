package services.parsers;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import models.Almond;
import models.Almond.AlmondVariety;
import models.Grower;
import models.Handler;

import services.DateService;
import services.GrowerService;
import services.HandlerService;

/**
 * Class to parse json data to create new Offer.
 *
 * Expected Json Structure:
 *  {
 *    HANDLER_ID: ... , (Constant in JsonParser super class)
 *
 *    GROWER_IDS: [
 *      ... ,
 *      ... ,
 *      ...
 *    ],
 *
 *    ALMOND_VARIETY: ... ,
 *
 *    ALMOND_POUNDS: ... ,
 *
 *    (TODO Determine Date Format)
 *    PAYMENT_DATE: ... ,
 *  }
 */
public class OfferJsonParser extends JsonParser {

  private Handler handler;
  private List<Grower> growers;
  private AlmondVariety almondVariety;
  private Integer almondPounds;
  private LocalDate paymentDate;

  public OfferJsonParser(JsonNode data) {
    super();

    handler = parseHandler(data);
    if (handler == null) {
      // Parser set to invalid with proper error message.
      return;
    }

    growers = parseGrowers(data);
    if (growers == null) {
      // Parser set to invalid with proper error message.
      return;
    }

    almondVariety = parseAlmondVariety(data);
    if (almondVariety == null) {
      // Parser set to invalid with proper error message.
      return;
    }

    almondPounds = parseAlmondPounds(data);
    if (almondPounds == null) {
      // Parser set to invalid with proper error message.
      return;
    }

    paymentDate = parsePaymentDate(data);
    if (paymentDate == null) {
      // Parser set to invalid with proper error message.
      return;
    }

    // Valid json data recieved
    setValid();
  }

  public Handler getHandler() {
    ensureValid();
    return handler;
  }

  public List<Grower> getGrowers() {
    ensureValid();
    return growers;
  }

  public AlmondVariety getAlmondVariety() {
    ensureValid();
    return almondVariety;
  }

  public Integer getAlmondPounds() {
    ensureValid();
    return almondPounds;
  }

  public LocalDate getPaymentDate() {
    ensureValid();
    return paymentDate;
  }

  private List<Grower> parseGrowers(JsonNode data) {
    // Check grower ids are present.
    if (!data.has(OfferJsonConstants.GROWER_IDS)) {
      setInvalid(missingParameterError(OfferJsonConstants.GROWER_IDS));
      return null;

    } 
    
    JsonNode growerIds = data.get(OfferJsonConstants.GROWER_IDS);

    // Grower IDs should be formatted as an array of strings.
    if (!growerIds.isArray()) {
      setInvalid("Grower ID Format Invalid: array of strings expected.");
      return null;
    }

    List<Grower> processedGrowers = new ArrayList<>();

    for (JsonNode node : growerIds) {
      Long growerId = parseLong(node.asText());

      // Ensure grower id is valid integer format.
      if (growerId == null) {
        setInvalid("Grower id value [" + node.asText() + "] is not a valid long integer.\n");
        return null;
      }

      // Check grower exists with given id.
      Grower grower = GrowerService.getGrower(growerId);
      if (grower == null) {
        setInvalid("Grower does not exist with grower id [" + growerId + "].\n");
        return null;
      }

      // Ensure given handler owns grower
      if (!HandlerService.checkHandlerOwnsGrower(handler, grower)) {
        setInvalid("Handler with id [ " + handler.getId() + " ] does not own grower with id [ "
            + growerId + " ].\n");
        return null;

      }

      processedGrowers.add(grower);
    }

    return processedGrowers;
  }

  private AlmondVariety parseAlmondVariety(JsonNode data) {
    // Check almound variety is preseent.
    if (!data.has(OfferJsonConstants.ALMOND_VARIETY)) {
      setInvalid(missingParameterError(OfferJsonConstants.ALMOND_VARIETY));
      return null;
    } 
    
    AlmondVariety almondVariety =
        Almond.stringToAlmondVariety(data.get(OfferJsonConstants.ALMOND_VARIETY).asText());

    if (almondVariety == null) {
      setInvalid("Almond Variety Format Invalid: string of valid almond variety expected.\n");
      return null;
    }

    return almondVariety;
  }

  private Integer parseAlmondPounds(JsonNode data) {
    // Check almound pounds is preseent.
    if (!data.has(OfferJsonConstants.ALMOND_POUNDS)) {
      setInvalid(missingParameterError(OfferJsonConstants.ALMOND_POUNDS));
      return null;

    } 
    
    Long almondPounds = parseLong(data.get(OfferJsonConstants.ALMOND_POUNDS).asText());

    if (almondPounds == null) {
      setInvalid("Almond Pound Format Invalid: integer expected.\n");
      return null;
    }

    return almondPounds.intValue();
  }

  private LocalDate parsePaymentDate(JsonNode data) {
    // Check payment date is preseent.
    if (!data.has(OfferJsonConstants.PAYMENT_DATE)) {
      setInvalid(missingParameterError(OfferJsonConstants.PAYMENT_DATE));
      return null;

    } 
    
    String dateString = data.get(OfferJsonConstants.PAYMENT_DATE).asText();

    if (!DateService.verifyDateString(dateString)) {
      // TODO: Determine Date Format
      setInvalid("Date String Format Invalid: string with TODO format expected.\n");
      return null;
    }

    return DateService.stringToDate(dateString);
  }

  private static class OfferJsonConstants {
    private static final String GROWER_IDS = "grower_ids";

    private static final String ALMOND_VARIETY = "almond_variety";
    private static final String ALMOND_POUNDS = "almond_pounds";

    private static final String PAYMENT_DATE = "payment_date";
  }
}