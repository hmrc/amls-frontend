package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RedressScheemsSpec extends PlaySpec with MockitoSugar {

  "RedressScheemsSpec" must {

    "validate model with few check box selected" in {
      val model = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressScheme.formRule.validate(model) must
        be(Success(RedressRegisteredYes(OmbudsmanServices)))

    }

  }
}
