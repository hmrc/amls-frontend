package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CommonHelperTest extends UnitSpec with MockitoSugar  {

  def mockedMessages(key:String):String = {
    key match {
      case "aaa.1" => "one"
      case "aaa.2" => "two"
      case "aaa.3" => "three"
      case _ => key
    }
  }

  "getSeqFromMessagesKey" must {
    "get a sequence of key values that exist in mocked messages file" in {
      val allKeys = Seq(
        "one" ->"one",
        "two" -> "two",
        "three" -> "three"
        )
      CommonHelper.getSeqFromMessagesKey("aaa", mockedMessages ) shouldBe allKeys
    }
  }
}