package views.responsiblepeople

import models.{Country, DateOfChange}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.MustMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.Tables.Table
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

import scala.collection.JavaConversions._

class detailed_answersSpec extends GenericTestHelper with MustMatchers with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {

    "have correct title" in new ViewFixture {
      def view = views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople()), 1, true)

      doc.title must startWith(Messages("responsiblepeople.detailed_answers.title") + " - " + Messages("summary.responsiblepeople"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople()), 1, true)

      heading.html must be(Messages("responsiblepeople.detailed_answers.title"))
      subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    def checkListContainsItems(parent: Element, keysToFind: Set[String]) = {
      val texts = parent.select("li").toSet.map((el: Element) => el.text())
      texts must be(keysToFind.map(k => Messages(k)))
      true
    }

    def checkElementTextIncludes(el: Element, keys: String*) = {
      val t = el.text()
      keys.foreach { k =>
        t must include(Messages(k))
      }
      true
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      (Messages("responsiblepeople.detailed_answers.previous_names"), checkElementTextIncludes(_, "firstName middleName lastName")),
      (Messages("responsiblepeople.detailed_answers.previous_names"), checkElementTextIncludes(_, "24 February 1990")),
      (Messages("responsiblepeople.detailed_answers.other_names"), checkElementTextIncludes(_, "otherName")),
      (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "AA346464B")),
      (Messages("responsiblepeople.detailed_answers.country_of_birth"), checkElementTextIncludes(_, "Uganda")),
      (Messages("lbl.nationality"), checkElementTextIncludes(_, "United Kingdom")),
      (Messages("responsiblepeople.detailed_answers.phone_number"), checkElementTextIncludes(_, "098765")),
      (Messages("responsiblepeople.detailed_answers.email"), checkElementTextIncludes(_, "e@mail.com")),
      (Messages("responsiblepeople.detailed_answers.address"), checkElementTextIncludes(_, "addressLine1 addressLine2 addressLine3 addressLine4 postCode1")),
      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "0 to 5 months")),
      (Messages("responsiblepeople.detailed_answers.previous_address"), checkElementTextIncludes(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
      //      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_,  "6 to 11 months")),
      (Messages("responsiblepeople.detailed_answers.other_previous_address"), checkElementTextIncludes(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3")),
      //      (Messages("responsiblepeople.timeataddress.address_history.heading"), checkElementTextIncludes(_, "EUR")),
      (Messages("responsiblepeople.detailed_answers.position"), checkElementTextIncludes(_, "Beneficial owner")),
      (Messages("responsiblepeople.detailed_answers.position"), checkElementTextIncludes(_, "Nominated officer")),
      (Messages("responsiblepeople.detailed_answers.position_start"), checkElementTextIncludes(_, "24 February 1990")),
      (Messages("responsiblepeople.detailed_answers.registered_for_vat"), checkElementTextIncludes(_, "No")),
      (Messages("responsiblepeople.detailed_answers.registered_for_sa"), checkElementTextIncludes(_, "Registered for Self Assessment")),
      (Messages("responsiblepeople.detailed_answers.previous_experience"), checkElementTextIncludes(_, "experience")),
      (Messages("responsiblepeople.detailed_answers.training_in_anti_money_laundering"), checkElementTextIncludes(_, "training")),
      (Messages("responsiblepeople.detailed_answers.already_passed_fit_and_proper"), checkElementTextIncludes(_, "Yes"))
    )

    val previousName = PreviousName(
      Some("firstName"),
      Some("middleName"),
      Some("lastName"),
      new LocalDate(1990, 2, 24)
    )

    val personName = PersonName(
      "firstName",
      Some("middleName"),
      "lastName",
      Some(previousName),
      Some("otherName")
    )

    val residenceType = PersonResidenceType(
      UKResidence("AA346464B"),
      Country("Uganda", "UG"),
      Some(Country("United Kingdom", "GB"))
    )

    val personAddress1 = PersonAddressUK(
      "addressLine1",
      "addressLine2",
      Some("addressLine3"),
      Some("addressLine4"),
      "postCode1"
    )
    val personAddress2 = PersonAddressUK(
      "addressLine5",
      "addressLine6",
      Some("addressLine7"),
      Some("addressLine8"),
      "postCode2"
    )
    val personAddress3 = PersonAddressUK(
      "addressLine9",
      "addressLine10",
      Some("addressLine11"),
      Some("addressLine12"),
      "postCode3"
    )

    val currentAddress = ResponsiblePersonCurrentAddress(
      personAddress = personAddress1,
      timeAtAddress = Some(ZeroToFiveMonths),
      dateOfChange = Some(DateOfChange(new LocalDate(1990, 2, 24)))
    )

    val additionalAddress = ResponsiblePersonAddress(
      personAddress = personAddress2,
      timeAtAddress = Some(SixToElevenMonths)
    )

    val additionalExtraAddress = ResponsiblePersonAddress(
      personAddress = personAddress3,
      timeAtAddress = Some(OneToThreeYears)
    )

    val addressHistory = ResponsiblePersonAddressHistory(
      currentAddress = Some(currentAddress),
      additionalAddress = Some(additionalAddress),
      additionalExtraAddress = Some(additionalExtraAddress)
    )

    val positions = Positions(
      positions = Set(BeneficialOwner, NominatedOfficer),
      startDate = Some(new LocalDate(1990, 2, 24))
    )

    val responsiblePeopleModel = ResponsiblePeople(
      personName = Some(personName),
      personResidenceType = Some(residenceType),
      contactDetails = Some(ContactDetails("098765", "e@mail.com")),
      addressHistory = Some(addressHistory),
      positions = Some(positions),
      vatRegistered = Some(VATRegisteredNo),
      saRegistered = Some(SaRegisteredYes("sa")),
      experienceTraining = Some(ExperienceTrainingYes("experience")),
      training = Some(TrainingYes("training")),
      hasAlreadyPassedFitAndProper = Some(true)
    )

    "include the provided data for a full uk address history" in new ViewFixture {
      def view = {
        views.html.responsiblepeople.detailed_answers(Some(responsiblePeopleModel), 1, true)
      }

      forAll(sectionChecks) { (key, check) => {
        val headers = doc.select("section.check-your-answers h2")
        val header = headers.toList.find(e => e.text() == key)

        header must not be None
        val section = header.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }

    "include the provided data for a single non-uk address" in new ViewFixture {

      val nonUkresponsiblePeopleModel = responsiblePeopleModel.copy(
        addressHistory = Some(
          ResponsiblePersonAddressHistory(
            currentAddress = Some(ResponsiblePersonCurrentAddress(
              personAddress = PersonAddressNonUK(
                "addressLine1","addressLine2",Some("addressLine3"),Some("addressLine4"),Country("spain","esp")
              ),
              timeAtAddress = Some(ZeroToFiveMonths),
              dateOfChange = Some(DateOfChange(new LocalDate(1990, 2, 24)))
            ))
          )
        )
      )

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        (Messages("responsiblepeople.detailed_answers.address"), checkElementTextIncludes(_, "addressLine1 addressLine2 addressLine3 addressLine4 spain")),
        (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "0 to 5 months")),
        (Messages("responsiblepeople.detailed_answers.previous_address"), checkElementTextIncludes(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
        //      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_,  "6 to 11 months")),
        (Messages("responsiblepeople.detailed_answers.other_previous_address"), checkElementTextIncludes(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3"))
        //      (Messages("responsiblepeople.timeataddress.address_history.heading"), checkElementTextIncludes(_, "EUR")),
      )

      def view = {
        views.html.responsiblepeople.detailed_answers(Some(nonUkresponsiblePeopleModel), 1, true)
      }
      
      forAll(sectionChecks) { (key, check) => {
        val headers = doc.select("section.check-your-answers h2")
        val header = headers.toList.find(e => e.text() == key)

        if(key.equals(Messages("responsiblepeople.detailed_answers.address")) || key.equals(Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"))){
          header must not be None
          val section = header.get.parents().select("section").first()
          check(section) must be(true)
        } else {
          header mustBe None
        }

      }
      }
    }

    "display address on separate lines" in new Fixture {
      def view = {
        views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople(addressHistory = Some(addressHistory))), 1, true)
      }

      def checkElementHasAttribute(el: Element, keys: String*) = {
        val t = el.text()
        keys.foreach { k =>
          t must include(Messages(k))
        }
        el.getElementsByTag("ul").first().hasClass("list--comma")
      }


      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        (Messages("responsiblepeople.detailed_answers.address"), checkElementHasAttribute(_, "addressLine1 addressLine2 addressLine3 addressLine4 postCode1")),
        (Messages("responsiblepeople.detailed_answers.previous_address"), checkElementHasAttribute(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
        (Messages("responsiblepeople.detailed_answers.other_previous_address"), checkElementHasAttribute(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3"))
      )

      forAll(sectionChecks) { (key, check) => {
        val headers = doc.select("section.check-your-answers h2")
        val header = headers.toList.find(e => e.text() == key)

        header must not be None
        val section = header.get.parents().select("section").first()
        check(section) must be(false)
      }
      }
    }
  }
}
