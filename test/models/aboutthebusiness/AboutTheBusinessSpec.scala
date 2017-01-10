package models.aboutthebusiness

import models.Country
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, Json}
import sun.util.resources.LocaleData

class AboutTheBusinessSpec extends PlaySpec with MockitoSugar {

  val previouslyRegistered = PreviouslyRegisteredYes("12345678")

  val regForVAT = VATRegisteredYes("123456789")

  // scalastyle:off
  val  activityStartDate = ActivityStartDate(new LocalDate(1990, 2, 24))

  val  newActivityStartDate = ActivityStartDate(new LocalDate(1990, 2, 24))

  val regForCorpTax = CorporationTaxRegisteredYes("1234567890")

  val contactingYou = ContactingYou("1234567890", "test@test.com")

  val regOfficeOrMainPlaceUK =  RegisteredOfficeUK("38B", "Longbenton", None, None, "NE7 7DX")

  val uKCorrespondenceAddress = UKCorrespondenceAddress("Name",
    "Business Name",
    "address 1",
    "address 2",
    Some("address 3"),
    Some("address 4"),
    "NE77 0QQ")

  val completeModel = AboutTheBusiness(
    previouslyRegistered = Some(previouslyRegistered),
    activityStartDate = Some(activityStartDate),
    vatRegistered = Some(regForVAT),
    corporationTaxRegistered = Some(regForCorpTax),
    contactingYou = Some(contactingYou),
    registeredOffice = Some(regOfficeOrMainPlaceUK),
    correspondenceAddress = Some(uKCorrespondenceAddress)
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
      "addressLine2" -> "Longbenton",
      "addressLine3" -> JsNull,
      "addressLine4" -> JsNull,
      "postCode" -> "NE7 7DX",
      "dateOfChange" -> JsNull),
    "correspondenceAddress" -> Json.obj(
      "yourName" -> "Name",
      "businessName" -> "Business Name",
      "correspondenceAddressLine1" -> "address 1",
      "correspondenceAddressLine2" -> "address 2",
      "correspondenceAddressLine3" -> "address 3",
      "correspondenceAddressLine4" -> "address 4",
      "correspondencePostCode" -> "NE77 0QQ"
    ),
    "hasChanged" -> false
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
      "hasChanged" -> false
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
  }

  "'None'" when {

    val initial: Option[AboutTheBusiness] = None

    "Merged with previously registered with MLR" must {
      "return AboutTheBusiness with correct previously registered for MLR option" in {
        val result = initial.previouslyRegistered(previouslyRegistered)
        result must be (AboutTheBusiness(Some(previouslyRegistered), None, None, None, None, None, None, true))
      }
    }

    "Merged with RegisteredForVAT" must {
      "return AboutTheBusiness with correct VAT Registered option" in {
        val result = initial.vatRegistered(regForVAT)
        result must be (AboutTheBusiness(None, None, Some(regForVAT), None, None, None, None, true))
      }
    }

    "Merged with CorporationTaxRegistered" must {
      "return AboutTheBusiness with correct corporation tax registered option" in {
        val result = initial.corporationTaxRegistered(regForCorpTax)
        result must be (AboutTheBusiness(None, None, None, Some(regForCorpTax), None, None, None, true))
      }
    }

    "Merged with RegisteredOfficeOrMainPlaceOfBusiness" must {
      "return AboutTheBusiness with correct registeredOfficeOrMainPlaceOfBusiness" in {
        val result = initial.registeredOffice(regOfficeOrMainPlaceUK)
        result must be (AboutTheBusiness(None, None, None, None, None, Some(regOfficeOrMainPlaceUK), None, true))
      }
    }

    "Merged with UKCorrespondenceAddress" must {
      "return AboutTheBusiness with correct UKCorrespondenceAddress" in {
        val result = initial.correspondenceAddress(uKCorrespondenceAddress)
        result must be (AboutTheBusiness(None, None, None, None, None, None, Some(uKCorrespondenceAddress), true))
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
          val res = completeModel.contactingYou(ContactingYou("9876655564", "new@testvalue.com"))
          res.hasChanged must be (true)
          res.contactingYou must be (Some(ContactingYou("9876655564", "new@testvalue.com")))
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