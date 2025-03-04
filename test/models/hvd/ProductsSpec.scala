/*
 * Copyright 2024 HM Revenue & Customs
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

package models.hvd

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import models.hvd.Products._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ProductsSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerTest {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .build()

  "Products" must {
    "sort itemtypes alphabetically" when {
      "other value is defined" in {
        val products = Products(
          Set(
            Alcohol,
            Tobacco,
            Antiques,
            Cars,
            OtherMotorVehicles,
            Caravans,
            Jewellery,
            Gold,
            ScrapMetals,
            MobilePhones,
            Clothing,
            Other("Nougat")
          )
        )

        products.sorted mustBe Seq(
          Alcohol,
          Antiques,
          Caravans,
          Cars,
          Clothing,
          Gold,
          Jewellery,
          MobilePhones,
          OtherMotorVehicles,
          ScrapMetals,
          Tobacco,
          Other("Nougat")
        )
      }
      "other value is not defined" in {
        val products = Products(
          Set(
            Alcohol,
            Tobacco,
            Antiques,
            Cars,
            OtherMotorVehicles,
            Caravans,
            Jewellery,
            Gold,
            ScrapMetals,
            MobilePhones,
            Clothing
          )
        )

        products.sorted mustBe Seq(
          Alcohol,
          Antiques,
          Caravans,
          Cars,
          Clothing,
          Gold,
          Jewellery,
          MobilePhones,
          OtherMotorVehicles,
          ScrapMetals,
          Tobacco
        )
      }
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json = Json.obj("products" -> Seq("06", "07", "08", "02", "01", "11"))

        Json.fromJson[Products](json) must
          be(JsSuccess(Products(Set(Clothing, Jewellery, Alcohol, Caravans, Gold, Tobacco)), JsPath))
      }
      "successfully validate given all values" in {
        val json = Json.obj("products" -> Seq("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"))

        Json.fromJson[Products](json) must
          be(
            JsSuccess(
              Products(
                Set(
                  MobilePhones,
                  Clothing,
                  Jewellery,
                  ScrapMetals,
                  Alcohol,
                  Caravans,
                  Gold,
                  Tobacco,
                  Antiques,
                  Cars,
                  OtherMotorVehicles
                )
              ),
              JsPath
            )
          )
      }

      "successfully validate given values with option other details" in {
        val json = Json.obj("products" -> Seq("09", "12"), "otherDetails" -> "test")

        Json.fromJson[Products](json) must
          be(JsSuccess(Products(Set(Other("test"), ScrapMetals)), JsPath))
      }

      "fail when on path is missing" in {
        Json.fromJson[Products](Json.obj("product" -> Seq("01"))) must
          be(JsError((JsPath \ "products") -> play.api.libs.json.JsonValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[Products](Json.obj("products" -> Seq("40"))) must
          be(JsError((JsPath \ "products") -> play.api.libs.json.JsonValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        Json.toJson[Products](Products(Set(Tobacco, Other("test657")))) must be(
          Json.obj("products" -> Json.arr("02", "12"), "otherDetails" -> "test657")
        )
      }
    }

    "getMessage" must {
      import play.api.i18n._
      implicit val lang = Lang("en-US")

      "return correct text for Alcohol" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Alcohol.getMessage must be("Alcohol")
      }

      "return correct text for Tobacco" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Tobacco.getMessage must be("Tobacco")
      }

      "return correct text for Antiques" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Antiques.getMessage must be("Antiques")
      }

      "return correct text for Cars" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Cars.getMessage must be("Cars")
      }

      "return correct text for OtherMotorVehicles" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        OtherMotorVehicles.getMessage must be("Motor vehicles (except cars)")
      }

      "return correct text for Caravans" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Caravans.getMessage must be("Caravans")
      }

      "return correct text for Jewellery" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Jewellery.getMessage must be("Jewellery")
      }

      "return correct text for Gold" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Gold.getMessage must be("Gold")
      }

      "return correct text for ScrapMetals" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        ScrapMetals.getMessage must be("Scrap metals")
      }

      "return correct text for MobilePhones" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        MobilePhones.getMessage must be("Mobile phones")
      }

      "return correct text for Clothing" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Clothing.getMessage must be("Clothing")
      }

      "return correct text for Other with empty string" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Other("").getMessage must be("Other things")
      }

      "return correct text for Other" in {
        val messagesApi                 = app.injector.instanceOf[MessagesApi]
        implicit val messages: Messages = messagesApi.preferred(Seq(lang))
        Other("Something else").getMessage must be("Something else")
      }
    }
  }

}
