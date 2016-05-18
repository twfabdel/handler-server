package models;

import com.avaje.ebean.Model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import models.interfaces.PrettyString;
import play.data.validation.Constraints;

@Entity
public class Handler extends Model implements PrettyString {

  @Id
  @Constraints.Min(10)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = DBConstants.HandlerColumns.ID)
  private Long id;

  public Long getId() {
    return id;
  }

  @Constraints.Required
  @Column(name = DBConstants.HandlerColumns.COMPANY_NAME)
  private String companyName;

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getCompanyName() {
    return companyName;
  }

  /* ============== TODO Authorization Here =============== */

  @OneToMany
  @Constraints.Required
  private List<Grower> growersList;

  public List<Grower> getGrowersList() {
    return growersList;
  }

  @OneToMany
  @Constraints.Required
  private List<Offer> offerList;

  public List<Offer> getOfferList() {
    return offerList;
  }

  public static Finder<Long, Handler> find = new Finder<>(Handler.class);

  public Handler(String companyName) {
    this.companyName = companyName;
    growersList = new ArrayList<>();
    offerList = new ArrayList<>();
  }

  @Override
  public String toString() {
    return "(" + id + ") " + companyName + " : " + getGrowersList().size();
  }
  
  public String toPrettyString() {
    StringBuilder builder = new StringBuilder();
    builder.append("(" + id + ") " + companyName + ":\n");

    if (growersList.isEmpty()) {
      builder.append(" [] ");

    } else {
      for (Grower grower : growersList) {
        builder.append("-- " + grower.toPrettyString());
      }
    }

    builder.append("\n");

    return builder.toString();
  }
}
