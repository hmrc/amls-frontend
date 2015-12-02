package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class CommonHelperTest extends UnitSpec with MockitoSugar {

  def mockedMessages(key: String): String = {
    key match {
      case "aaa.1" => "one"
      case "aaa.2" => "two"
      case "aaa.3" => "three"
      case _ => key
    }
  }

  "mapSeqWithMessagesKey" must {
    "map a sequence with key values that exist in mocked messages file" in {
      val allKeys = Seq("one" -> "1", "two" -> "2", "three" -> "3")
      CommonHelper.mapSeqWithMessagesKey(Seq("1", "2", "3"), "aaa", mockedMessages) shouldBe allKeys
    }
  }
}
