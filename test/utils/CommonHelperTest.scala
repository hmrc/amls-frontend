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

  "mapSeqWithMessagesKey" must {
    "map a sequence with key values that exist in mocked messages file" in {
      val allKeys = Seq("1"->"one", "2"->"two", "3"->"three")
      CommonHelper.mapSeqWithMessagesKey(Seq("1", "2", "3" ), "aaa", mockedMessages ) shouldBe allKeys
    }
  }

}
