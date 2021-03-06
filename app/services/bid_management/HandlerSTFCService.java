package services.bid_management;

import java.time.Duration;

import java.util.*;

import models.HandlerBid;
import models.BaseBid.BidStatus;
import models.BidResponseResult;
import models.Grower;

import akka.actor.Cancellable;
import scala.concurrent.duration.FiniteDuration;
import java.util.concurrent.TimeUnit;

import services.impl.EbeanGrowerService;
import services.GrowerService;
import services.messaging.bid.HandlerBidSendGridMessageService;
import services.messaging.bid.HandlerBidSMSMessageService;

import play.libs.Akka;

public class HandlerSTFCService implements HandlerBidManagementService {


  private final HandlerBid handlerBid;
  
  private Cancellable cancellable;
  private long poundsRemaining;
  private List<Long> growerIdsRemaining;
  GrowerService growerService = new EbeanGrowerService();
  HandlerBidSendGridMessageService emailService = new HandlerBidSendGridMessageService();
  HandlerBidSMSMessageService smsService = new HandlerBidSMSMessageService();

  public HandlerSTFCService(HandlerBid handlerBid, Duration timeAllowed) {
    this.handlerBid = handlerBid;
    this.poundsRemaining = handlerBid.getAlmondPounds();

    growerIdsRemaining = getGrowerIDList();

    emailService.send(handlerBid);
    smsService.send(handlerBid);
    
    HandlerBidManagementService.bidToManageService.put(handlerBid, this);

    cancellable = Akka.system().scheduler()
        .scheduleOnce(FiniteDuration.create(timeAllowed.toMinutes(), TimeUnit.MINUTES), new Runnable() {
          @Override
          public void run() {
            if (poundsRemaining == handlerBid.getAlmondPounds()) {
              handlerBid.closeBid(BidStatus.REJECTED);
            } else {
              handlerBid.closeBid(BidStatus.PARTIAL);
            }
          }
        }, Akka.system().dispatcher());
  }

  private List<Long> getGrowerIDList() {
    List<Long> growers = new ArrayList<>();
    for (Grower grower : handlerBid.getAllGrowers()) {
      growers.add(grower.getId());
    }
    return growers;
  }

  @Override
  public void addGrowers(List<Long> growerIds) {
    growerIdsRemaining.addAll(growerIds);
  }

  @Override
  public BidResponseResult accept(long pounds, long growerId) {
    if (!checkPoundsRemaining(pounds)) {
      return BidResponseResult.getInvalidResult("Only " + poundsRemaining
        + " pounds remain. Can not accept bid for " + pounds + " pounds.");
    }

    growerIdsRemaining.remove((Long) growerId);

    return BidResponseResult.getValidResult();
  }

  @Override
  public BidResponseResult reject(long growerId) {
    growerIdsRemaining.remove((Long) growerId);

    return BidResponseResult.getValidResult();
  }

  @Override
  public void close() {
    cancellable.cancel();
  }

  @Override
  public BidResponseResult approve(long pounds, long growerId) {
    if (!checkPoundsRemaining(pounds)) {
      return BidResponseResult.getInvalidResult("Only " + poundsRemaining
        + " pounds remain. Can not approve bid for " + pounds + " pounds.");
    }

    poundsRemaining -= pounds;

    if (poundsRemaining == 0) {
      handlerBid.closeBid(BidStatus.ACCEPTED);
      sendClosedToRemaining(); 
      cancellable.cancel();
    } else {
      sendUpdatedToRemaining();
    }

    return BidResponseResult.getValidResult();
  }

  @Override
  public BidResponseResult disapprove(long growerrId) {
    return BidResponseResult.getValidResult();
  }

  private boolean checkPoundsRemaining(long pounds) {
    if (pounds > poundsRemaining) {
      return false;
    } 
    return true;
  }

  private void sendClosedToRemaining() {
    for(Long growerId: growerIdsRemaining) {
      Grower g = growerService.getById(growerId);
      emailService.sendClosed(handlerBid, g);
      smsService.sendClosed(handlerBid, g);  
    }
  }

  private void sendUpdatedToRemaining() {
    for(Long growerId: growerIdsRemaining) {
      Grower g = growerService.getById(growerId);
      emailService.sendUpdated(handlerBid, g, formatUpdateMessage());
      smsService.sendUpdated(handlerBid, g, formatUpdateMessage());  
    }
  }

  private String formatUpdateMessage(){
    return "Your bid number " + Long.toString(handlerBid.getId()) + " has been updated. \n"
        + "\tBid number " + Long.toString(handlerBid.getId()) + " now contains the following specs: \n"
        + "\t\tAlmond type: " + handlerBid.getAlmondVariety() +"\n\t\tPrice per pound: " 
        + handlerBid.getPricePerPound() + "\n\t\tPOUNDS REMAINING: " 
        + Long.toString(poundsRemaining);
  }
}