/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.estateagentbusiness

import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json._



class ServicesSpec extends PlaySpec with MockitoSugar {

  "ServicesSpec" must {

    val businessServices:Set[Service] = Set(Residential, Commercial, Auction, Relocation,
                                            BusinessTransfer, AssetManagement, LandManagement, Development, SocialHousing)
    import jto.validation.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "services[]" -> Seq("01","02","03","04","05","06","07","08","09")
      )

      Services.formReads.validate(model) must
        be(Valid(Services(businessServices)))

    }

    "fail to validate on empty Map" in {

      Services.formReads.validate(Map.empty) must
        be(Invalid(Seq((Path \ "services") -> Seq(ValidationError("error.required.eab.business.services")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services[]" -> Seq("02", "99", "03")
      )

      Services.formReads.validate(model) must
        be(Invalid(Seq((Path \ "services"\ 1 \ "services", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      Services.formWrites.writes(Services(Set(Residential, Commercial, Auction))) must
        be(Map("services[]" -> Seq("01","02","03")))

      Services.formWrites.writes(Services(Set(AssetManagement, BusinessTransfer, LandManagement))) must
        be(Map("services[]" -> Seq("06","05","07")))

      Services.formWrites.writes(Services(Set(Relocation, Development, SocialHousing))) must
        be(Map("services[]" -> Seq("04","08","09")))

    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("services" -> Seq("01","02","03","04","05","06","07","08","09"),
          "dateOfChange" -> "2016-02-24")

        Json.fromJson[Services](json) must
          be(JsSuccess(Services(businessServices, Some(DateOfChange(new LocalDate(2016,2,24)))), JsPath))
      }

      "fail when on invalid data" in {
        Json.fromJson[Services](Json.obj("services" -> Seq("40"))) must
          be(JsError(((JsPath \ "services")(0) \ "services") -> play.api.data.validation.ValidationError("error.invalid")))
      }
    }

    "successfully validate json write" in {
      val json = Json.obj("services" -> Set("01","02","03","04","05","06","07","08","09"))
      Json.toJson(Services(businessServices)) must be(json)

    }
  }
}
