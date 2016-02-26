package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping. Success


class TransactionTypeSpec extends PlaySpec with MockitoSugar {

  "TransactionType" must {

    import play.api.data.mapping.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("test")
      )

      TransactionRecord.formReads.validate(model) must
        be(Success(TransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))))

    }
  }
}
