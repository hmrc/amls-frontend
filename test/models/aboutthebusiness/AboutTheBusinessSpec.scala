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

package models.aboutthebusiness

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsNull, Json}
import play.api.test.FakeApplication

class AboutTheBusinessSpec extends PlaySpec with MockitoSugar  with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.has-accepted" -> true))

  val previouslyRegistered = PreviouslyRegisteredYes("12345678")

  val regForVAT = VATRegisteredYes("123456789")

  // scalastyle:off
  val  activityStartDate = ActivityStartDate(new LocalDate(1990, 2, 24))

  val  newActivityStartDate = ActivityStartDate(new LocalDate(1990, 2, 24))

  val regForCorpTax = CorporationTaxRegisteredYes("1234567890")

  val contactingYou = ContactingYou(Some("1234567890"), Some("test@test.com"))

  val regOfficeOrMainPlaceUK =  RegisteredOfficeUK("38B", "line2", None, None, "AA1 1AA")

  val uKCorrespondenceAddress = UKCorrespondenceAddress("Name",
    "Business Name",
    "address 1",
    "address 2",
    Some("address 3"),
    Some("address 4"),
    "AA11 1AA")

  val completeModel = AboutTheBusiness(
    previouslyRegistered = Some(previouslyRegistered),
    activityStartDate = Some(activityStartDate),
    vatRegistered = Some(regForVAT),
    corporationTaxRegistered = Some(regForCorpTax),
    contactingYou = Some(contactingYou),
    registeredOffice = Some(regOfficeOrMainPlaceUK),
    altCorrespondenceAddress = Some(true),
    correspondenceAddress = Some(uKCorrespondenceAddress),
    hasAccepted = true
  )

  val completeJson = Json.obj(
    "previouslyRegistered" -> Json.obj("previouslyRegistered" -> true,
      "prevMLRRegNo" -> "12345678"),
    "activityStartDate" -> Json.obj(
      "startDate" -> "1990-02-24"),
    "vatRegistered" -> Json.obj("registeredForVAT" -> true,
      "vrnNumber" -> "123456789"),
    "corporationTaxRegistered" -> Json.obj("registeredForCorporationTax" -> true,
      "corporationTaxReference" -> "1234567890"),
    "contactingYou" -> Json.obj(
      "phoneNumber" -> "1234567890",
      "email" -> "test@test.com"),
    "registeredOffice" -> Json.obj(
      "addressLine1" -> "38B",
      "addressLine2" -> "line2",
      "addressLine3" -> JsNull,
      "addressLine4" -> JsNull,
      "postCode" -> "AA1 1AA",
      "dateOfChange" -> JsNull),
    "altCorrespondenceAddress" -> true,
    "correspondenceAddress" -> Json.obj(
      "yourName" -> "Name",
      "businessName" -> "Business Name",
      "correspondenceAddressLine1" -> "address 1",
      "correspondenceAddressLine2" -> "address 2",
      "correspondenceAddressLine3" -> "address 3",
      "correspondenceAddressLine4" -> "address 4",
      "correspondencePostCode" -> "AA11 1AA"
    ),
    "hasChanged" -> false,
    "hasAccepted" -> true
  )

  "AboutTheBusiness Serialisation" must {
    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[AboutTheBusiness] must
        be(completeModel)
    }

    "isComplete must return true" in {
      completeModel.isComplete must be(true)
    }
  }

  it when {
    "hasChanged is missing from the Json" must {
      "Deserialise correctly" in {
        (completeJson - "hasChanged").as[AboutTheBusiness] must
          be (completeModel)
      }
    }
  }

  "Partially complete AboutTheBusiness" must {

    val partialJson = Json.obj(
      "previouslyRegistered" -> Json.obj("previouslyRegistered" -> true,
      "prevMLRRegNo" -> "12345678"
      ),
      "hasChanged" -> false,
      "hasAccepted" -> false
    )

    val partialModel = AboutTheBusiness(Some(previouslyRegistered), None)

    "Serialise as expected" in {
      Json.toJson(partialModel) must
        be(partialJson)
    }

    "Deserialise as expected" in {
      partialJson.as[AboutTheBusiness] must
        be(partialModel)
    }

    "isComplete must return false" in {
      partialModel.isComplete must be(false)
    }
  }

  "'None'" when {

    val initial: Option[AboutTheBusiness] = None

    "Merged with previously registered with MLR" must {
      "return AboutTheBusiness with correct previously registered for MLR option" in {
        val result = initial.previouslyRegistered(previouslyRegistered)
        result must be (AboutTheBusiness(Some(previouslyRegistered), None, None, None, None, None, None, None, true))
      }
    }

    "Merged with RegisteredForVAT" must {
      "return AboutTheBusiness with correct VAT Registered option" in {
        val result = initial.vatRegistered(regForVAT)
        result must be (AboutTheBusiness(None, None, Some(regForVAT), None, None, None, None, None, true))
      }
    }

    "Merged with CorporationTaxRegistered" must {
      "return AboutTheBusiness with correct corporation tax registered option" in {
        val result = initial.corporationTaxRegistered(regForCorpTax)
        result must be (AboutTheBusiness(None, None, None, Some(regForCorpTax), None, None, None, None, true))
      }
    }

    "Merged with RegisteredOfficeOrMainPlaceOfBusiness" must {
      "return AboutTheBusiness with correct registeredOfficeOrMainPlaceOfBusiness" in {
        val result = initial.registeredOffice(regOfficeOrMainPlaceUK)
        result must be (AboutTheBusiness(None, None, None, None, None, Some(regOfficeOrMainPlaceUK), None, None, true))
      }
    }

    "Merged with UKCorrespondenceAddress" must {
      "return AboutTheBusiness with correct UKCorrespondenceAddress" in {
        val result = initial.correspondenceAddress(uKCorrespondenceAddress)
        result must be (AboutTheBusiness(None, None, None, None, None, None, None, Some(uKCorrespondenceAddress), true))
      }
    }
  }

  "AboutTheBusiness class" when {
    "previouslyRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.previouslyRegistered(previouslyRegistered)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val res = completeModel.previouslyRegistered(PreviouslyRegisteredNo)
          res.hasChanged must be (true)
          res.previouslyRegistered must be (Some(PreviouslyRegisteredNo))
        }
      }
    }

    "activityStartDate value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.activityStartDate(activityStartDate)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & activityStartDate Properties" in {
          val res = completeModel.activityStartDate(ActivityStartDate(new LocalDate(1344, 12, 1)))
          res.hasChanged must be (true)
          res.activityStartDate must be (Some(ActivityStartDate(new LocalDate(1344, 12, 1))))
        }
      }
    }

    "vatRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.vatRegistered(regForVAT)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & vatRegistered Properties" in {
          val res = completeModel.vatRegistered(VATRegisteredNo)
          res.hasChanged must be (true)
          res.vatRegistered must be (Some(VATRegisteredNo))
        }
      }
    }

    "corporationTaxRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.corporationTaxRegistered(regForCorpTax)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & corporationTaxRegistered Properties" in {
          val res = completeModel.corporationTaxRegistered(CorporationTaxRegisteredYes("3333333333"))
          res.hasChanged must be (true)
          res.corporationTaxRegistered must be (Some(CorporationTaxRegisteredYes("3333333333")))
        }
      }
    }

    "contactingYou value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.contactingYou(contactingYou)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & contactingYou Properties" in {
          val res = completeModel.contactingYou(ContactingYou(Some("0000000000"), Some("new@testvalue.com")))
          res.hasChanged must be (true)
          res.contactingYou must be (Some(ContactingYou(Some("0000000000"), Some("new@testvalue.com"))))
        }
      }
    }

    "registeredOffice value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.registeredOffice(regOfficeOrMainPlaceUK)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & registeredOffice Properties" in {
          val res = completeModel.registeredOffice(RegisteredOfficeUK("Line 1 New", "Line 2 New", None, None, "NEW CODE"))
          res.hasChanged must be (true)
          res.registeredOffice must be (Some(RegisteredOfficeUK("Line 1 New", "Line 2 New", None, None, "NEW CODE")))
        }
      }
    }

    "correspondenceAddress value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val res = completeModel.correspondenceAddress(uKCorrespondenceAddress)
          res must be (completeModel)
          res.hasChanged must be (false)
        }
      }

      "is different" must {
        "set the hasChanged & correspondenceAddress Properties" in {
          val res = completeModel.correspondenceAddress(UKCorrespondenceAddress("name new", "Business name new", "Line 1 New", "Line 2 New", None, None, "NEW CODE"))
          res.hasChanged must be (true)
          res.correspondenceAddress must be (Some(UKCorrespondenceAddress("name new", "Business name new", "Line 1 New", "Line 2 New", None, None, "NEW CODE")))
        }
      }
    }

  }
}