/*
 * Copyright 2020 HM Revenue & Customs
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
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessMatching, MoneyServiceBusiness}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.{Country, DateOfChange}
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.responsiblepeople.detailed_answers

import scala.collection.JavaConversions._

class detailed_answersSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks with ResponsiblePeopleValues {

  trait ViewFixture extends Fixture {
    lazy val detailed_answers = app.injector.instanceOf[detailed_answers]
    implicit val requestWithToken = addTokenForView(FakeRequest())

    val businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      (Messages("responsiblepeople.detailed_answers.uk_resident", personName.fullName), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.uk_resident.nino", personName.fullName), checkElementTextIncludes(_, nino)),
      (Messages("responsiblepeople.country.of.birth.heading", personName.fullName), checkElementTextIncludes(_, "No")),
      (Messages("responsiblepeople.detailed_answers.country_of_birth", personName.fullName), checkElementTextIncludes(_, "Uganda")),
      (Messages("responsiblepeople.nationality.heading", personName.fullName), checkElementTextIncludes(_, "British (including English, Scottish, Welsh and Northern Irish)")),
      (Messages("responsiblepeople.contact_details.heading", personName.fullName), checkElementTextIncludes(_, "Telephone number: 098765 Email address: e@mail.com")),
      (Messages("responsiblepeople.detailed_answers.address", personName.fullName), checkElementTextOnlyIncludes(_, "addressLine1 addressLine2 addressLine3 addressLine4 postCode1")),
      (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "0 to 5 months")),
      (Messages("responsiblepeople.detailed_answers.address.previous.UK", "firstName middleName lastName"), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.address.previous", "firstName middleName lastName"), checkElementTextIncludes(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
      (Messages("responsiblepeople.detailed_answers.address.other.previous.UK", "firstName middleName lastName"), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.address.other.previous", "firstName middleName lastName"), checkElementTextIncludes(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3")),
      (Messages("responsiblepeople.position_within_business.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "Beneficial owner (holding more than 25% of shares in the business)")),
      (Messages("responsiblepeople.position_within_business.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "Nominated officer")),
      (Messages("responsiblepeople.position_within_business.startDate.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "24 February 1990")),
      (Messages("responsiblepeople.sole.proprietor.another.business.heading", personName.fullName), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.registeredforvat.heading", personName.fullName), checkElementTextIncludes(_, "No")),
      (Messages("responsiblepeople.registeredforselfassessment.heading", personName.fullName), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.registered_for_sa"), checkElementTextIncludes(_, "sa")),
      (Messages("responsiblepeople.experiencetraining.heading", "firstName middleName lastName", "a money service business"), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.previous_experience.detail", "firstName middleName lastName"), checkElementTextIncludes(_, "experience")),
      (Messages("responsiblepeople.training.heading", personName.fullName), checkElementTextIncludes(_, "Yes")),
      (Messages("responsiblepeople.detailed_answers.training_in_anti_money_laundering", personName.fullName), checkElementTextIncludes(_, "training")),
      (Messages("responsiblepeople.fit_and_proper.heading", personName.fullName), checkElementTextIncludes(_, "Yes"))
    )
  }

  "summary view" must {

    "have correct title" in new ViewFixture {
      def view = detailed_answers(Some(ResponsiblePerson()), 1, true, businessMatching = businessMatching)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.responsiblepeople"))
    }

    "have correct headings" in new ViewFixture {
      def view = detailed_answers(Some(ResponsiblePerson()), 1, true, businessMatching = businessMatching)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    "include the provided data" when {

      "a full uk address history" in new ViewFixture {
        def view = {
          detailed_answers(Some(responsiblePeopleModel), 1, true, personName.fullName, businessMatching = businessMatching)
        }

        val element = doc.getElementsMatchingOwnText(Messages("responsiblepeople.detailed_answer.tell.us.moved", personName.fullName))
        element.hasAttr("href") must be(true)
        element.attr("href") must be("/anti-money-laundering/responsible-people/date-change-moved/1/")


        forAll(sectionChecks) { (key, check) => {
            val questions = doc.select("span.bold")
            val question = questions.toList.find(e => e.text() == Messages(key))
            question must not be None
            val section = question.get.parents().select("div").first()
            check(section) must be(true)
          }
        }
      }

      "seperate experience heading test" in new ViewFixture {
        def view = {
          detailed_answers(Some(responsiblePeopleModel), 1, true, personName.fullName, businessMatching = businessMatching.copy(activities = Some(BusinessActivities(Set(MoneyServiceBusiness, AccountancyServices)))))
        }

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.experiencetraining.heading.multiple", "firstName middleName lastName"), checkElementTextIncludes(_, "experience"))
        )

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
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

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.detailed_answers.address", personName.fullName), checkElementTextIncludes(_, "addressLine1 addressLine2 addressLine3 addressLine4 spain")),
          (Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"), checkElementTextIncludes(_, "0 to 5 months")),
          (Messages("responsiblepeople.detailed_answers.previous_address"), checkElementTextIncludes(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
          (Messages("responsiblepeople.detailed_answers.other_previous_address"), checkElementTextIncludes(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3"))
        )

        def view = {
          detailed_answers(Some(nonUkresponsiblePeopleModel), 1, false, personName.fullName, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          if (key.equals(Messages("responsiblepeople.detailed_answers.address", personName.fullName)) ||
            key.equals(Messages("responsiblepeople.timeataddress.address_history.heading", personName.fullName))
          ) {
            question must not be None
            val section = question.get.parents().select("div").first()
            check(section) must be(true)
          } else {
            question mustBe None
          }
        }
        }
      }

      "a non-uk resident, non-uk passport" in new ViewFixture {

        val nonUKPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
          personResidenceType = Some(PersonResidenceType(
            NonUKResidence,
            Some(Country("Uganda", "UG")),
            Some(Country("Italy", "ITA"))
          )),
          ukPassport = Some(UKPassportNo),
          nonUKPassport = Some(NonUKPassportYes("0000000000"))
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.detailed_answers.uk_resident", personName.fullName), checkElementTextIncludes(_, "No")),
          (Messages("responsiblepeople.detailed_answers.uk.passport", personName.fullName), checkElementTextIncludes(_, "No")),
          (Messages("responsiblepeople.detailed_answers.non.uk.passport", personName.fullName), checkElementTextIncludes(_, "Yes")),
          (Messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName.fullName), checkElementTextIncludes(_, "00000000"))
        )

        def view = {
          detailed_answers(Some(nonUKPassportResponsiblePeopleModel), 1, false, personName.fullName, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
          check(section) must be(true)
        }
        }
      }

      "date-of-birth" in new ViewFixture {

        val responsiblePeopleModelWithDOB = responsiblePeopleModel.copy(
          dateOfBirth = Some(DateOfBirth(LocalDate.parse("2000-01-01"))),
          ukPassport = Some(UKPassportNo)
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.date.of.birth.heading", personName.fullName), checkElementTextIncludes(_, "1 January 2000"))
        )

        def view = {
          detailed_answers(Some(responsiblePeopleModelWithDOB), 1, false, personName.fullName, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
          check(section) must be(true)
        }
        }

      }

      "a non-uk resident, uk passport" in new ViewFixture {

        val ukPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
          personResidenceType = Some(PersonResidenceType(
            NonUKResidence,
            Some(Country("Uganda", "UG")),
            Some(Country("Italy", "ITA"))
          )),
          ukPassport = Some(UKPassportYes("0000000000"))
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.detailed_answers.uk_resident", personName.fullName), checkElementTextIncludes(_, "No")),
          (Messages("responsiblepeople.detailed_answers.uk.passport", personName.fullName), checkElementTextIncludes(_, "Yes")),
          (Messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName.fullName), checkElementTextIncludes(_, "0000000000")))

        def view = {
          detailed_answers(Some(ukPassportResponsiblePeopleModel), 1, false, personName.fullName, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
          check(section) must be(true)
        }
        }

      }

      "a non-uk resident, no passport" in new ViewFixture {

        val ukPassportResponsiblePeopleModel = responsiblePeopleModel.copy(
          personResidenceType = Some(PersonResidenceType(
            NonUKResidence,
            Some(Country("Uganda", "UG")),
            Some(Country("Italy", "ITA"))
          )),
          ukPassport = Some(UKPassportNo),
          nonUKPassport = Some(NoPassport)
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.detailed_answers.uk_resident", personName.fullName), checkElementTextIncludes(_, "No")),
          (Messages("responsiblepeople.detailed_answers.non.uk.passport", personName.fullName), checkElementTextIncludes(_, "No"))
        )

        def view = {
          detailed_answers(Some(ukPassportResponsiblePeopleModel), 1, false, personName.fullName, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
          check(section) must be(true)
        }
        }

      }

      "approval check was paid" in new ViewFixture {
        val responsiblePeopleModelWithApprovalCheck = responsiblePeopleModel.copy(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true))
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.approval_check.heading", personName.fullName), checkElementTextIncludes(_, "Yes"))
        )

        def view = {
          detailed_answers(Some(responsiblePeopleModelWithApprovalCheck), 1, true, personName.fullName, showApprovalSection = true, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question must not be None
          val section = question.get.parents().select("div").first()
          check(section) must be(true)
        }
        }
      }

      "approval check question is not shown if show flag" in new ViewFixture {
        val responsiblePeopleModelWithApprovalCheck = responsiblePeopleModel.copy(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true))
        )

        override val sectionChecks = Table[String, Element => Boolean](
          ("title key", "check"),
          (Messages("responsiblepeople.detailed_answers.already_paid_approval_check", personName.fullName), checkElementTextIncludes(_, "Yes"))
        )

        def view = {
          detailed_answers(Some(responsiblePeopleModelWithApprovalCheck), 1, true, personName.fullName, showApprovalSection = false, businessMatching = businessMatching)
        }

        forAll(sectionChecks) { (key, check) => {
          val questions = doc.select("span.bold")
          val question = questions.toList.find(e => e.text() == Messages(key))
          question mustBe None
        }
        }
      }
    }

    "display address on separate lines" in new ViewFixture {
      def view = detailed_answers(Some(ResponsiblePerson(addressHistory = Some(addressHistory))), 1, true, rpName = personName.fullName, businessMatching = businessMatching)

      def checkElementHasAttribute(el: Element, keys: String*) = {
        val t = el.text()
        keys.foreach { k =>
          t must include(Messages(k))
        }
        el.getElementsByTag("ul").first().hasClass("list--comma")
      }

      override val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        (Messages("responsiblepeople.detailed_answers.address", personName.fullName), checkElementHasAttribute(_, "addressLine1 addressLine2 addressLine3 addressLine4 postCode1")),
        (Messages("responsiblepeople.detailed_answers.address.previous", personName.fullName), checkElementHasAttribute(_, "addressLine5 addressLine6 addressLine7 addressLine8 postCode2")),
        (Messages("responsiblepeople.detailed_answers.address.other.previous", personName.fullName), checkElementHasAttribute(_, "addressLine9 addressLine10 addressLine11 addressLine12 postCode3"))
      )

      forAll(sectionChecks) { (key, check) => {
        val questions = doc.select("span.bold")
        val question = questions.toList.find(e => e.text() == Messages(key))
        question must not be None
        val section = question.get.parents().select("div").first()
        check(section) must be(false)
      }
      }
    }

    "display timeAtAddress for corresponding addresses" in new ViewFixture {

      val responsiblePeople = ResponsiblePerson(personName = Some(personName), addressHistory = Some(addressHistory))

      def view = detailed_answers(Some(responsiblePeople), 1, false, personName.fullName, businessMatching = businessMatching)

      val timeAtAddresses = doc.getElementsMatchingOwnText(Messages("responsiblepeople.timeataddress.address_history.heading", "firstName middleName lastName"))

      timeAtAddresses(0).parent().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.5_months_history"))
      timeAtAddresses(2).parent().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.11_months_history"))
      timeAtAddresses(4).parent().nextElementSibling().text() must be(Messages("responsiblepeople.timeataddress.3_years_history"))

    }

    "have the correct href" in new ViewFixture {
      def view = {
        detailed_answers(Some(responsiblePeopleModel), 1, true, rpName = personName.fullName, businessMatching = businessMatching)
      }

      override val sectionChecks = Table[String, Element => Boolean](
        ("title key", "check"),
        (Messages("responsiblepeople.detailed_answers.address", personName.fullName), checkElementTextIncludes(_, "/anti-money-laundering/responsible-people/moved-address/1"))
      )

      def checkElementTextIncludes(el:Element, keys : String*) = {
        val l = el.getElementsByTag("a").attr("href")
        keys.foreach { k =>
          l must include(Messages(k))
        }
        true
      }

      forAll(sectionChecks) { (key, check) => {
        val questions = doc.select("span.bold")
        val question = questions.toList.find(e => e.text() == Messages(key))
        question must not be None
        val section = question.get.parents().select("div").first()
        check(section) must be(true)
      }
      }
    }

  }
}

trait ResponsiblePeopleValues extends NinoUtil {

  val nino = nextNino

  val previousName = PreviousName(
    Some(true),
    Some("firstName"),
    Some("middleName"),
    Some("lastName")
  )

  val personName = PersonName(
    "firstName",
    Some("middleName"),
    "lastName"
  )

  val residenceType = PersonResidenceType(
    UKResidence(Nino(nino)),
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
    startDate = Some(PositionStartDate(new LocalDate(1990, 2, 24)))
  )

  val responsiblePeopleModel = ResponsiblePerson(
    personName = Some(personName),
    personResidenceType = Some(residenceType),
    contactDetails = Some(ContactDetails("098765", "e@mail.com")),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    vatRegistered = Some(VATRegisteredNo),
    saRegistered = Some(SaRegisteredYes("sa")),
    experienceTraining = Some(ExperienceTrainingYes("experience")),
    training = Some(TrainingYes("training")),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))
  )
}