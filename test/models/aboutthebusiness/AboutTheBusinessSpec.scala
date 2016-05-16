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

  "AboutTheBusiness" must {
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
      "postCode" -> "NE7 7DX"),
      "correspondenceAddress" -> Json.obj(
      "yourName" -> "Name",
      "businessName" -> "Business Name",
      "correspondenceAddressLine1" -> "address 1",
      "correspondenceAddressLine2" -> "address 2",
      "correspondenceAddressLine3" -> "address 3",
      "correspondenceAddressLine4" -> "address 4",
      "correspondencePostCode" -> "NE77 0QQ"
        )
      )


    val completeModel = AboutTheBusiness(
      previouslyRegistered = Some(PreviouslyRegisteredYes("12345678")),
      activityStartDate = Some(activityStartDate),
      vatRegistered = Some(regForVAT),
      corporationTaxRegistered = Some(regForCorpTax),
      contactingYou = Some(contactingYou),
      registeredOffice = Some(regOfficeOrMainPlaceUK),
      correspondenceAddress = Some(uKCorrespondenceAddress)
    )

    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[AboutTheBusiness] must
        be(completeModel)
    }
  }

  "Partially complete AboutTheBusiness" must {

    val partialJson = Json.obj(
      "previouslyRegistered" -> true,
      "prevMLRRegNo" -> "12345678"
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

  "None" when {

    val initial: Option[AboutTheBusiness] = None

    "Merged with previously registered with MLR" must {
      "return AboutTheBusiness with correct previously registered for MLR option" in {
        val result = initial.previouslyRegistered(previouslyRegistered)
        result must be (AboutTheBusiness(Some(previouslyRegistered), None))
      }
    }

    "Merged with RegisteredForVAT" must {
      "return AboutTheBusiness with correct VAT Registered option" in {
        val result = initial.vatRegistered(regForVAT)
        result must be (AboutTheBusiness(None, None, Some(regForVAT), None, None, None))
      }
    }

    "Merged with CorporationTaxRegistered" must {
      "return AboutTheBusiness with correct corporation tax registered option" in {
        val result = initial.corporationTaxRegistered(regForCorpTax)
        result must be (AboutTheBusiness(None, None, None, Some(regForCorpTax), None, None, None))
      }
    }

    "Merged with RegisteredOfficeOrMainPlaceOfBusiness" must {
      "return AboutTheBusiness with correct registeredOfficeOrMainPlaceOfBusiness" in {
        val result = initial.registeredOffice(regOfficeOrMainPlaceUK)
        result must be (AboutTheBusiness(None, None, None, None, None, Some(regOfficeOrMainPlaceUK)))
      }
    }

    "Merged with UKCorrespondenceAddress" must {
      "return AboutTheBusiness with correct UKCorrespondenceAddress" in {
        val result = initial.correspondenceAddress(uKCorrespondenceAddress)
        result must be (AboutTheBusiness(None, None, None, None, None, None, Some(uKCorrespondenceAddress)))
      }
    }
  }


  "AboutTheBusiness" when {

    "previouslyRegistered already set" when {

      val initial = AboutTheBusiness(Some(previouslyRegistered), None)

      "Merged with previously registered with MLR" must {
        "return AboutTheBusiness with correct previously registered status" in {
          val newPreviouslyRegistered = PreviouslyRegisteredYes("22222222")
          val result = initial.previouslyRegistered(newPreviouslyRegistered)
          result must be (AboutTheBusiness(Some(newPreviouslyRegistered), None))
        }
      }

      "Merged with RegisteredForVAT" must {
        "return AboutTheBusiness with correct VAT registration number" in {
          val newregForVAT = VATRegisteredYes("012345678")
          val result = initial.vatRegistered(newregForVAT)
          result must be (AboutTheBusiness(Some(previouslyRegistered), None, Some(newregForVAT)))
        }
      }

      "Merged with RegisteredOfficeOrMainPlaceOfBusiness" must {
        "return AboutTheBusiness with correct registeredOfficeOrMainPlaceOfBusiness" in {
          val newregOffice = RegisteredOfficeNonUK("38B", "Longbenton", None, None, Country("United Kingdom", "GB"))
          val result = initial.registeredOffice(newregOffice)
          result must be (AboutTheBusiness(Some(previouslyRegistered), None, None, None, None, Some(newregOffice)))
        }
      }
    }

    "AboutTheBusiness" when {

      "regForVAT and regOfficeOrMainPlaceUK already set" when {

        val initial = AboutTheBusiness(None, Some(activityStartDate), Some(regForVAT), None, None, Some(regOfficeOrMainPlaceUK))

        "return AboutTheBusiness with correct VAT registration number" must {
          "return AboutTheBusiness with correct previously registered status" in {
            val newPreviouslyRegistered = PreviouslyRegisteredYes("22222222")
            val result = initial.previouslyRegistered(newPreviouslyRegistered)
            result must be(AboutTheBusiness(Some(newPreviouslyRegistered), Some(activityStartDate), Some(regForVAT), None, None,  Some(regOfficeOrMainPlaceUK)))
          }
        }

        "Merged with RegisteredForVAT" must {
          "Merged with previously registered with MLR" in {
            val newregForVAT = VATRegisteredYes("012345678")
            val result = initial.vatRegistered(newregForVAT)
            result must be(AboutTheBusiness(None, Some(activityStartDate), Some(newregForVAT), None, None, Some(regOfficeOrMainPlaceUK)))
          }
        }

        "Merged with RegisteredOfficeOrMainPlaceOfBusiness" must {
          "return AboutTheBusiness with correct registered office detailes" in {
            val newregOffice = RegisteredOfficeNonUK("38B", "Longbenton", None, None, Country("United Kingdom", "GB"))
            val result = initial.registeredOffice(newregOffice)
            result must be(AboutTheBusiness(None, Some(activityStartDate), Some(regForVAT), None, None, Some(newregOffice)))
          }
        }
      }
    }
  }
}