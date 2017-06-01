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

package views.responsiblepeople

import controllers.responsiblepeople.NinoUtil
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
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class detailed_answersSpec extends GenericTestHelper
  with MustMatchers
  with TableDrivenPropertyChecks
  with HtmlAssertions
  with NinoUtil {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {

    val nino = nextNino

    "have correct title" in new ViewFixture {
      def view = views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople()), 1, true)

      doc.title must startWith(Messages("responsiblepeople.detailed_answers.title") + " - " + Messages("summary.responsiblepeople"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople()), 1, true)

      heading.html must be(Messages("responsiblepeople.detailed_answers.title"))
      subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      (Messages("responsiblepeople.detailed_answers.previous_names"), checkElementTextIncludes(_, "firstName middleName lastName")),
      (Messages("responsiblepeople.detailed_answers.previous_names"), checkElementTextIncludes(_, "24 February 1990")),
      (Messages("responsiblepeople.detailed_answers.other_names"), checkElementTextIncludes(_, "otherName")),
      (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, nino)),
      (Messages("responsiblepeople.detailed_answers.country_of_birth"), checkElementTextIncludes(_, "Uganda")),
      (Messages("lbl.nationality"), checkElementTextIncludes(_, "United Kingdom")),
      (Messages("responsiblepeople.detailed_answers.phone_number"), checkElementTextIncludes(_, "098765")),
      (Messages("responsiblepeople.detailed_answers.email"), checkElementTextIncludes(_, "e@mail.com")),
      (Messages("responsiblepeople.detailed_answers.address"), checkElementTextOnlyIncludes(_, "addressLine1 addressLine2 addressLine3 addressLine4 postCode1")),
      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "0 to 5 months")),
      (Messages("responsiblepeople.detailed_answers.previous_address"), checkElementTextIncludes(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
      //      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_,  "6 to 11 months")),
      (Messages("responsiblepeople.detailed_answers.other_previous_address"), checkElementTextIncludes(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3")),
      //      (Messages("responsiblepeople.timeataddress.address_history.heading"), checkElementTextIncludes(_, "EUR")),
      (Messages("responsiblepeople.detailed_answers.position"), checkElementTextIncludes(_, "Beneficial owner")),
      (Messages("responsiblepeople.detailed_answers.position"), checkElementTextIncludes(_, "Nominated officer")),
      (Messages("responsiblepeople.detailed_answers.position_start"), checkElementTextIncludes(_, "24 February 1990")),
      (Messages("responsiblepeople.detailed_answers.soleproprietor_for_other_business"), checkElementTextIncludes(_, "Yes")),
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
      UKResidence(nino),
      Some(Country("Uganda", "UG")),
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
      hasAlreadyPassedFitAndProper = Some(true),
      soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))
    )

    "include the provided data" when {

      "a full uk address history" in new ViewFixture {
        def view = {
          views.html.responsiblepeople.detailed_answers(Some(responsiblePeopleModel), 1, true, true)
        }

        val element = doc.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
        element.hasAttr("href") must be(true)
        element.attr("href") must be("/anti-money-laundering/responsible-people/date-change-moved/1")

        forAll(sectionChecks) { (key, check) => {
          val headers = doc.select("section.check-your-answers h2")
          val header = headers.toList.find(e => e.text() == key)

          header must not be None
          val section = header.get.parents().select("section").first()
          check(section) must be(true)
        }
        }
      }

      "a single non-uk address" in new ViewFixture {

        val nonUkresponsiblePeopleModel = responsiblePeopleModel.copy(
          addressHistory = Some(
            ResponsiblePersonAddressHistory(
              currentAddress = Some(ResponsiblePersonCurrentAddress(
                personAddress = PersonAddressNonUK(
                  "addressLine1", "addressLine2", Some("addressLine3"), Some("addressLine4"), Country("spain", "esp")
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

          if (key.equals(Messages("responsiblepeople.detailed_answers.address")) || key.equals(Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"))) {
            header must not be None
            val section = header.get.parents().select("section").first()
            check(section) must be(true)
          } else {
            header mustBe None
          }

        }
        }
      }

//      "a non-uk resident, non-uk passport" in new ViewFixture {
//
//        val nonUKPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
//          personResidenceType = Some(PersonResidenceType(
//            NonUKResidence(new LocalDate(1990, 2, 25), NonUKPassport("0000000000")),
//            Country("Uganda", "UG"),
//            Some(Country("Italy", "ITA"))
//          ))
//        )
//
//        val sectionChecks = Table[String, Element => Boolean](
//          ("title key", "check"),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "No")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "25 February 1990")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "Passport Number: 0000000000"))
//        )
//
//        def view = {
//          views.html.responsiblepeople.detailed_answers(Some(nonUKPassportResponsiblePeopleModel), 1, true)
//        }
//
//        forAll(sectionChecks) { (key, check) => {
//          val headers = doc.select("section.check-your-answers h2")
//          val header = headers.toList.find(e => e.text() == key)
//
//          header must not be None
//          val section = header.get.parents().select("section").first()
//          check(section) must be(true)
//
//        }
//        }
//
//      }
//
//      "a non-uk resident, uk passport" in new ViewFixture {
//
//        val ukPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
//          personResidenceType = Some(PersonResidenceType(
//            NonUKResidence(new LocalDate(1990, 2, 25), UKPassport("0000000000")),
//            Country("Uganda", "UG"),
//            Some(Country("Italy", "ITA"))
//          ))
//        )
//
//        val sectionChecks = Table[String, Element => Boolean](
//          ("title key", "check"),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "No")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "25 February 1990")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "Passport Number: 0000000000")))
//
//        def view = {
//          views.html.responsiblepeople.detailed_answers(Some(ukPassportResponsiblePeopleModel), 1, true)
//        }
//
//        forAll(sectionChecks) { (key, check) => {
//          val headers = doc.select("section.check-your-answers h2")
//          val header = headers.toList.find(e => e.text() == key)
//
//          header must not be None
//          val section = header.get.parents().select("section").first()
//          check(section) must be(true)
//
//        }
//        }
//
//      }
//
//      "a non-uk resident, no passport" in new ViewFixture {
//
//        val ukPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
//          personResidenceType = Some(PersonResidenceType(
//            NonUKResidence(new LocalDate(1990, 2, 25), NoPassport),
//            Country("Uganda", "UG"),
//            Some(Country("Italy", "ITA"))
//          ))
//        )
//
//        val sectionChecks = Table[String, Element => Boolean](
//          ("title key", "check"),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "No")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "25 February 1990")),
//          (Messages("responsiblepeople.detailed_answers.uk_resident"), checkElementTextIncludes(_, "No passport"))
//        )
//
//        def view = {
//          views.html.responsiblepeople.detailed_answers(Some(ukPassportResponsiblePeopleModel), 1, true)
//        }
//
//        forAll(sectionChecks) { (key, check) => {
//          val headers = doc.select("section.check-your-answers h2")
//          val header = headers.toList.find(e => e.text() == key)
//
//          header must not be None
//          val section = header.get.parents().select("section").first()
//          check(section) must be(true)
//
//        }
//        }
//
//      }

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

    "display timeAtAddress for corresponding addresses" in new Fixture {
      def view = {
        views.html.responsiblepeople.detailed_answers(Some(ResponsiblePeople(personName = Some(personName), addressHistory = Some(addressHistory))), 1, true)
      }

      val timeAtAddresses = doc.getElementsMatchingOwnText(Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"))

      timeAtAddresses(0).nextElementSibling().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.5_months_history"))
      timeAtAddresses(2).nextElementSibling().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.11_months_history"))
      timeAtAddresses(4).nextElementSibling().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.3_years_history"))

    }

    "have the correct href" in new Fixture {
      def view = {
        views.html.responsiblepeople.detailed_answers(Some(responsiblePeopleModel), 1, true, true)
      }

      val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        (Messages("responsiblepeople.detailed_answers.address"), checkElementTextIncludes(_, "/anti-money-laundering/responsible-people/moved-address/1"))
      )

      def checkElementTextIncludes(el:Element, keys : String*) = {
        val l = el.getElementsByTag("a").attr("href")
        keys.foreach { k =>
          l must include(Messages(k))
        }
        true
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
  }
}
