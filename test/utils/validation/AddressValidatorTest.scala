package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.AddressValidator._

class AddressValidatorTest extends PlaySpec with MockitoSugar with WithFakeApplication  {

  "address" should {
    "respond suitably to all mandatory lines being blank" in {
      val allMandatoryBlank = Map(
        "addr1key"->"",
        "addr2key"->"",
        "postcodekey"->"CA3 9SD",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "mandatory-blank", "all-mandatory-blank", "invalid-line","blank-postcode","invalid-postcode").bind(allMandatoryBlank)
        .left.getOrElse(Nil).contains(FormError("", "all-mandatory-blank")) mustBe true
    }

    "respond suitably to any but not all mandatory lines being blank" in {
      val anyMandatoryBlank = Map(
        "addr1key"->"a",
        "addr2key"->"",
        "postcodekey"->"CA3 9SD",
        "countrycodekey"->"GB"
      )
      val mapping = address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "mandatory-blank", "all-mandatory-blank", "invalid-line","blank-postcode","invalid-postcode")
        .binder.bind("addr1key", anyMandatoryBlank).left.getOrElse(Nil)
        .contains(FormError("addr2key", "mandatory-blank")) mustBe true
    }

    "respond suitably to invalid lines" in {
      val invalidLine2 = Map(
        "addr1key"->"addr1",
        "addr2key"->"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"pcode",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "mandatory-blank", "all-mandatory-blank", "invalid-line","blank-postcode","invalid-postcode").bind(invalidLine2)
        .left.getOrElse(Nil).contains(FormError("addr2key", "invalid-line")) mustBe true
    }

    "respond suitably to blank postcode" in {
      val blankPostcode = Map(
        "addr1key"->"addr1",
        "addr2key"->"addr2",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "mandatory-blank", "all-mandatory-blank", "invalid-line","blank-postcode","invalid-postcode").bind(blankPostcode)
        .left.getOrElse(Nil).contains(FormError("postcodekey", "blank-postcode")) mustBe true
    }

    "respond suitably to invalid postcode" in {
      val invalidPostcode = Map(
        "addr1key"->"addr1",
        "addr2key"->"addr2",
        "addr3key"->"addr3",
        "addr4key"->"addr4",
        "postcodekey"->"CC!",
        "countrycodekey"->"GB"
      )
      address("addr2key","addr3key","addr4key","postcodekey", "countrycodekey",
        "mandatory-blank", "all-mandatory-blank", "invalid-line","blank-postcode","invalid-postcode").bind(invalidPostcode)
        .left.getOrElse(Nil).contains(FormError("postcodekey", "invalid-postcode")) mustBe true
    }
  }

}
