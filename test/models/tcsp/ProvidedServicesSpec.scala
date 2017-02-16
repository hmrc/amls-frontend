package models.tcsp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ProvidedServicesSpec extends PlaySpec with MockitoSugar {

  "Form Rules and Writes" must {

    "successfully validate given one selected values" in {
      val urlFormEncoded = Map("services[]" -> Seq("01"))
      val expected = Valid(ProvidedServices(Set(PhonecallHandling)))
      ProvidedServices.formReads.validate(urlFormEncoded) must be(expected)
    }

    "successfully validate given multiple selected values" in {
      val urlFormEncoded = Map("services[]" -> Seq("01", "02", "03", "04", "05", "06", "07"))
      val allServices = Set[TcspService](PhonecallHandling, EmailHandling, EmailServer,
                                         SelfCollectMailboxes, MailForwarding, Receptionist, ConferenceRooms)
      val expected = Valid(ProvidedServices(allServices))
      ProvidedServices.formReads.validate(urlFormEncoded) must be(expected)
    }

    "successfully validate given other value" in {
      val urlFormEncoded = Map("services[]" -> Seq("08"), "details" -> Seq("other service"))
      val expected = Valid(ProvidedServices(Set(Other("other service"))))
      ProvidedServices.formReads.validate(urlFormEncoded) must be(expected)
    }

    "successfully write given one values" in {
      val services = ProvidedServices(Set(PhonecallHandling))
      val expected = Map("services[]" -> Seq("01"))
      ProvidedServices.formWrites.writes(services) must be(expected)
    }

    "successfully write given multiple values" in {
      val services = ProvidedServices(Set(EmailServer, MailForwarding))
      val expected = Map("services[]" -> Seq("03", "05"))
      ProvidedServices.formWrites.writes(services) must be(expected)
    }

    "successfully write given 'other' value" in {
      val services = ProvidedServices(Set(Other("other service")))
      val expected = Map("services[]" -> Seq("08"), "details" -> Seq("other service"))
      ProvidedServices.formWrites.writes(services) must be(expected)
    }

    "show an error with an invalid value" in {
      val urlFormEncoded = Map("services[]" -> Seq("1738"))
      val expected = Invalid(Seq((Path \ "services") -> Seq(ValidationError("error.invalid"))))
      ProvidedServices.formReads.validate(urlFormEncoded) must be(expected)
    }

    "show an error with a missing details value" in {
      val form = Map("services[]" -> Seq("08"), "details" -> Seq(""))
      val expectedResult = Invalid(Seq(Path \ "details" -> Seq(ValidationError("error.required.tcsp.provided_services.details"))))

      ProvidedServices.formReads.validate(form) must be(expectedResult)
    }

    "show an error with an invalid details value" in {
      val form = Map("services[]" -> Seq("08"), "details" -> Seq("$3<>7485/45"))
      val expectedResult = Invalid(Seq(Path \ "details" -> Seq(ValidationError("err.text.validation"))))

      ProvidedServices.formReads.validate(form) must be(expectedResult)
    }


  }

  "Json read and writes" must {
    import play.api.data.validation.ValidationError
    "Serialise single service as expected" in {
      Json.toJson(ProvidedServices(Set(EmailServer))) must be(Json.obj("services" -> Seq("03")))
    }

    "Serialise multiple services as expected" in {
      val json = Json.obj("services" -> Seq("01", "02", "03", "04", "05", "06", "07"))
      val allServices = Set[TcspService](PhonecallHandling, EmailHandling, EmailServer,
        SelfCollectMailboxes, MailForwarding, Receptionist, ConferenceRooms)
      val result = Json.toJson(ProvidedServices(allServices)).\("services").as[Seq[String]]
      result must contain allOf ("01", "02", "03", "04", "05", "06", "07")
    }

    "Serialise 'other' service as expected" in {
      Json.toJson(ProvidedServices(Set(Other("other service")))) must be(Json.obj("services" -> Seq("08"), "details" -> "other service"))
    }

    "Deserialise single service as expected" in {
      val json = Json.obj("services" -> Set("01"))
      val expected = JsSuccess(ProvidedServices(Set(PhonecallHandling)), JsPath )
      Json.fromJson[ProvidedServices](json) must be (expected)
    }

    "Deserialise multiple service as expected" in {
      val json = Json.obj("services" -> Seq("01", "02", "03", "04", "05", "06", "07"))
      val allServices = Set[TcspService](PhonecallHandling, EmailHandling, EmailServer,
                                         SelfCollectMailboxes, MailForwarding, Receptionist, ConferenceRooms)
      val expected = JsSuccess(ProvidedServices(allServices), JsPath )
      Json.fromJson[ProvidedServices](json) must be (expected)
    }

    "Deserialise 'other' service as expected" in {
      val json = Json.obj("services" -> Set("08"), "details" -> "other service")
      val expected = JsSuccess(ProvidedServices(Set(Other("other service"))), JsPath )
      Json.fromJson[ProvidedServices](json) must be (expected)
    }

    "fail when on invalid data" in {
      Json.fromJson[ProvidedServices](Json.obj("transactions" -> Set("40"))) must
        be(JsError((JsPath \ "services") -> ValidationError("error.path.missing")))
    }

    "fail when on missing details data" in {
      Json.fromJson[ProvidedServices](Json.obj("transactions" -> Set("08"))) must
        be(JsError((JsPath \ "services") -> ValidationError("error.path.missing")))
    }

    "fail when on missing all data" in {
      Json.fromJson[ProvidedServices](Json.obj()) must
        be(JsError((JsPath \ "services") -> ValidationError("error.path.missing")))
    }
  }
}