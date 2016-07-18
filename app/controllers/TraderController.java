package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import java.util.List;

import play.Logger;

import models.HandlerSeller;
import models.Trader;
import models.TraderBid;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Status;

import controllers.security.TraderSecured;
import controllers.security.TraderSecurityController;

import services.HandlerSellerService;
import services.TraderService;
import services.TraderBidService;
import services.messaging.bid.BidMessageService;
import services.bid_management.FCFSService;
import services.bid_management.WaterfallService;
//import services.parsers.HandlerSellerJsonParser;
import services.parsers.TraderBidJsonParser;
import services.parsers.BidJsonParser.ManagementTypeInfo;

import utils.JsonMsgUtils;
import utils.ResponseHeaders;

@Security.Authenticated(TraderSecured.class)
public class TraderController extends Controller {

  private final TraderService traderService;
  private final HandlerSellerService handlerSellerService;
  private final TraderBidService traderBidService;
  private final BidMessageService bidMessageService;

  private final ObjectMapper jsonMapper;

  @Inject
  public TraderController(
      TraderService traderService,
      HandlerSellerService handlerSellerService,
      TraderBidService traderBidService,
      BidMessageService bidMessageService) {
    this.traderService = traderService;
    this.handlerSellerService = handlerSellerService;
    this.traderBidService = traderBidService;
    this.bidMessageService = bidMessageService;

    this.jsonMapper = new ObjectMapper();
  }

  public Result getTrader() {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    try {
      return ok(jsonMapper.writeValueAsString(trader));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  // Annotation ensures that POST request is of type application/json. If not HTTP 400 response
  // returned.
  @BodyParser.Of(BodyParser.Json.class)
  public Result createHandlerSeller() {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    JsonNode data = request().body().asJson();

    if (data == null) {
      return badRequest(JsonMsgUtils.expectingData());
    }

    addCurrentTraderId(trader, data);

    // HandlerSellerJsonParser parser = new HandlerSellerJsonParser(data);

    // if (!parser.isValid()) {
    //   return badRequest(JsonMsgUtils.caughtException(parser.getErrorMessage()));
    // }

    // if (!parser.getTrader().equals(trader)) {
    //   JsonMsgUtils.caughtException(
    //       "Can only create HandlerSellers that belong to "
    //       + trader.getCompanyName() + ".");
    // }

    // HandlerSeller handlerSeller = parser.formHandlerSeller();
    // handlerSeller.save();

    // try {
    //   return created(jsonMapper.writeValueAsString(handlerSeller));
    // } catch (JsonProcessingException e) {
    //   return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    // }
    return ok("In Progress.");

  }

  public Result getAllHandlerSellers() {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    try {
      return ok(jsonMapper.writeValueAsString(handlerSellerService.getByTrader(trader.getId())));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  public Result getHandlerSeller(long handlerSellerId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    HandlerSeller handlerSeller = handlerSellerService.getById(handlerSellerId);
    if (handlerSeller == null) {
      return notFound(JsonMsgUtils.handlerSellerNotFoundMessage(handlerSellerId));
    }

    if (!traderService.checkTraderOwnsHandlerSeller(trader, handlerSeller)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnHandlerMessage(trader, handlerSeller));
    }

    try {
      return ok(jsonMapper.writeValueAsString(handlerSeller));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  public Result deleteHandlerSeller(long handlerSellerId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    HandlerSeller handlerSeller = handlerSellerService.getById(handlerSellerId);
    if (handlerSeller == null) {
      return notFound(JsonMsgUtils.handlerSellerNotFoundMessage(handlerSellerId));
    }

    if (!traderService.checkTraderOwnsHandlerSeller(trader, handlerSeller)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnHandlerMessage(trader, handlerSeller));
    }

    for(TraderBid traderBid: traderBidService.getByHandlerSeller(handlerSellerId)) {
      if(traderBid.bidCurrentlyOpen()) {
        //Conflict response
        return status(409, JsonMsgUtils.handlerSellerInBid(handlerSellerId, traderBid.getId()));
      }
    }

    handlerSeller.delete();
    return ok(JsonMsgUtils.handlerSellerDeleted(handlerSellerId));
  }

  public Result updateHandlerSeller(long handlerSellerId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    JsonNode data = request().body().asJson();

    if (data == null) {
      return badRequest(JsonMsgUtils.expectingData());
    }

    addCurrentTraderId(trader, data);

    HandlerSeller handlerSeller = handlerSellerService.getById(handlerSellerId);
    if (handlerSeller == null) {
      return notFound(JsonMsgUtils.handlerSellerNotFoundMessage(handlerSellerId));
    }

    return ok("In Progress.");

    // HandlerSellerJsonParser parser = new HandlerSellerJsonParser(data);

    // if (!parser.isValid()) {
    //   return badRequest(JsonMsgUtils.caughtException(parser.getErrorMessage()));
    // }

    // if (!parser.getTrader().equals(trader)) {
    //   JsonMsgUtils.caughtException(
    //       "Can only update handlerSellers that belong to "
    //       + trader.getCompanyName() + ".");
    // }

    // if (!traderService.checkTraderOwnsHandlerSeller(trader, handlerSeller)) {
    //   return badRequest(JsonMsgUtils.traderDoesNotOwnHandlerMessage(trader, handlerSeller));
    // }

    // for(TraderBid traderBid: traderBidService.getByHandlerSeller(handlerSellerId)) {
    //   if(traderBid.bidCurrentlyOpen()) {
    //     //Conflict response
    //     return status(409, JsonMsgUtils.handlerSellerInBid(handlerSellerId, traderBid.getId()));
    //   }
    // }

    // parser.updateHandlerSeller(handlerSeller);
    // handlerSeller.save();

    // try {
    //   return created(jsonMapper.writeValueAsString(handlerSeller));
    // } catch (JsonProcessingException e) {
    //   return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    // }
  }

  // Annotation ensures that POST request is of type application/json. If not HTTP 400 response
  // returned.
  @BodyParser.Of(BodyParser.Json.class)
  public Result createBid() {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    JsonNode data = request().body().asJson();

    if (data == null) {
      return badRequest(JsonMsgUtils.expectingData());
    }

    addCurrentTraderId(trader, data);

    TraderBidJsonParser parser = new TraderBidJsonParser(data);

    if (!parser.isValid()) {
      return badRequest(JsonMsgUtils.caughtException(parser.getErrorMessage()));
    }

    if (!parser.getTrader().equals(trader)) {
      JsonMsgUtils.caughtException(
          "Can only create bids that belong to "
          + trader.getCompanyName() + ".");
    }

    TraderBid traderBid = parser.formBid();
    traderBid.save();

    // ManagementTypeInfo managementType = parser.getManagementType();
    // Class<?> classType = managementType.getClassType();

    // if (classType == WaterfallService.class) {
    //   new WaterfallService(traderBid, managementType.getDelay());
    // } else if (classType == FCFSService.class) {
    //   new FCFSService(traderBid, managementType.getDelay());
    // } else {
    //   return internalServerError(JsonMsgUtils.caughtException(classType.getName() 
    //     + " management type not found\n"));
    // }

    try {
      return created(jsonMapper.writeValueAsString(traderBid));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  public Result updateBid(long bidId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    JsonNode data = request().body().asJson();

    if (data == null) {
      return badRequest(JsonMsgUtils.expectingData());
    }

    addCurrentTraderId(trader, data);

    TraderBidJsonParser parser = new TraderBidJsonParser(data);

    if (!parser.isValid()) {
      return badRequest(JsonMsgUtils.caughtException(parser.getErrorMessage()));
    }

    if (!parser.getTrader().equals(trader)) {
      JsonMsgUtils.caughtException(
          "Can only create bids that belong to "
          + trader.getCompanyName() + ".");
    }
  
    TraderBid traderBid = traderBidService.getById(bidId);
    parser.updateBid(traderBid);
    traderBid.save();

    try {
      return created(jsonMapper.writeValueAsString(traderBid));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }    
  }

  public Result deleteBid(long bidId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    TraderBid traderBid = traderBidService.getById(bidId);
    if (traderBid == null) {
      return notFound(JsonMsgUtils.bidNotFoundMessage(bidId));
    }

    if (!traderService.checkTraderOwnsBid(trader, traderBid)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnBidMessage(trader, traderBid));
    }

    traderBid.delete();
    return ok(JsonMsgUtils.bidDeleted(bidId));
  }

  public Result getAllBids() {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    List<TraderBid> traderBids = traderBidService.getByTrader(trader.getId());

    // traderBids will be null if Trader with traderId cannot be found.
    if (traderBids == null) {
      return notFound(JsonMsgUtils.traderNotFoundMessage(trader.getId()));
    }

    try {
      return ok(jsonMapper.writeValueAsString(traderBids));
    } catch (JsonProcessingException e) {
      Logger.error(e.toString());
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  public Result getBid(long bidId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    TraderBid traderBid = traderBidService.getById(bidId);
    if (traderBid == null) {
      return notFound(JsonMsgUtils.bidNotFoundMessage(bidId));
    }

    if (!traderService.checkTraderOwnsBid(trader, traderBid)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnBidMessage(trader, traderBid));
    }

    try {
      return ok(jsonMapper.writeValueAsString(traderBid));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }

  public Result getHandlerSellersBids(long handlerSellerId) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }

    HandlerSeller handlerSeller = handlerSellerService.getById(handlerSellerId);
    if (handlerSeller == null) {
      return notFound(JsonMsgUtils.handlerSellerNotFoundMessage(handlerSellerId));
    }

    if (!traderService.checkTraderOwnsHandlerSeller(trader, handlerSeller)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnHandlerMessage(trader, handlerSeller));
    }

    List<TraderBid> traderBids = traderBidService.getByHandlerSeller(handlerSellerId);
    if (traderBids == null) {
      return notFound(
          JsonMsgUtils.caughtException("Could not get bids for handlerSeller with id " + handlerSellerId));
    }

    try {
      return ok(jsonMapper.writeValueAsString(traderBids));
    } catch (JsonProcessingException e) {
      return internalServerError(JsonMsgUtils.caughtException(e.toString()));
    }
  }
  
  public Result sendBid(long id) {
    ResponseHeaders.addResponseHeaders(response());

    Trader trader = TraderSecurityController.getTrader();

    if (trader == null) {
      return traderNotFound();
    }
    
    TraderBid traderBid = traderBidService.getById(id);
    if (traderBid == null) {
      return notFound(JsonMsgUtils.bidNotFoundMessage(id));
    }

    if (!traderService.checkTraderOwnsBid(trader, traderBid)) {
      return badRequest(JsonMsgUtils.traderDoesNotOwnBidMessage(trader, traderBid));
    }
    
    //boolean emailSuccess = bidMessageService.send(handlerBid);


    //return emailSuccess
    //    ? ok(JsonMsgUtils.successfullEmail())
    //    : internalServerError(JsonMsgUtils.emailsNotSent());
    return ok("In Progress.");
  }

  private Result traderNotFound() {
    return redirect("/trader/logout");
  }

  private void addCurrentTraderId(Trader trader, JsonNode data) {
    if (data.isObject()) {
      // TODO Change String to Literal to Constant.
      ((ObjectNode) data).put("trader_id", trader.getId());
    }
  }
}