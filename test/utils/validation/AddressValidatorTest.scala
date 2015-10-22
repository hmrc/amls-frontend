package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import play.api.data.FormError
import utils.validation.AddressValidator._

class AddressValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

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
        .left.getOrElse(Nil).contains(FormError("", "all-mandatory-blank")) shouldBe true
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
        .contains(FormError("addr2key", "mandatory-blank")) shouldBe true
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
        .left.getOrElse(Nil).contains(FormError("addr2key", "invalid-line")) shouldBe true
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
        .left.getOrElse(Nil).contains(FormError("postcodekey", "blank-postcode")) shouldBe true
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
        .left.getOrElse(Nil).contains(FormError("postcodekey", "invalid-postcode")) shouldBe true
    }
  }

}
