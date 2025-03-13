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

package views.responsiblepeople

import controllers.responsiblepeople.NinoUtil
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.{Country, DateOfChange}
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.AmlsSummaryViewSpec
import utils.responsiblepeople.CheckYourAnswersHelper
import views.Fixture
import views.html.responsiblepeople.CheckYourAnswersView

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks with ResponsiblePeopleValues {

  lazy val answersView = inject[CheckYourAnswersView]
  lazy val cyaHelper   = inject[CheckYourAnswersHelper]

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[_] = addTokenForView(FakeRequest())

    val businessMatching: BusinessMatching =
      BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))

    val cyaList: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
      responsiblePeopleModel,
      businessMatching,
      personName.fullName,
      1,
      None,
      true,
      true
    )
  }

  "CheckYourAnswersView" must {

    "have correct title" in new ViewFixture {
      def view = answersView(cyaList, 1, true, personName.fullName)

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.responsiblepeople"))
    }

    "have correct headings" in new ViewFixture {
      def view = answersView(cyaList, 1, true, personName.fullName)

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByClass("govuk-heading-m").asScala.zipWithIndex.foreach { case (subheading, index) =>
        subheading.text() mustBe messages(s"responsiblepeople.check_your_answers.subheading.${index + 1}")
      }
    }

    "include the provided data" when {

      "a full uk address history" in new ViewFixture {
        override def view: HtmlFormat.Appendable = answersView(cyaList, 1, true, personName.fullName)

        (
          cyaList.flatMap(_._2.rows) lazyZip
            doc.getElementsByClass("govuk-summary-list__key").asScala lazyZip
            doc.getElementsByClass("govuk-summary-list__value").asScala
        ).foreach { case (row, key, value) =>
          row.key.content.asHtml.body must include(key.text())

          val valueText = row.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
      }

      "a full non-uk address history" in new ViewFixture {

        val cyaListNonUK = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel.copy(addressHistory = Some(addressHistoryNonUK)),
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        override def view: HtmlFormat.Appendable = answersView(
          cyaListNonUK,
          1,
          true,
          personName.fullName
        )

        (
          cyaListNonUK.flatMap(_._2.rows) lazyZip
            doc.getElementsByClass("govuk-summary-list__key").asScala lazyZip
            doc.getElementsByClass("govuk-summary-list__value").asScala
        ).foreach { case (row, key, value) =>
          row.key.content.asHtml.body must include(key.text())

          val valueText = row.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
      }

      "ensure approval check question is not shown if flag is false" in new ViewFixture {
        val responsiblePeopleModelWithApprovalCheck = responsiblePeopleModel.copy(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true))
        )

        def view =
          answersView(
            cyaHelper.getHeadingsAndSummaryLists(
              responsiblePeopleModelWithApprovalCheck,
              businessMatching,
              personName.fullName,
              1,
              None,
              true,
              false
            ),
            1,
            true,
            personName.fullName,
            None
          )

        val elem = doc.getElementsContainingText(
          messages("responsiblepeople.detailed_answers.already_paid_approval_check", personName.fullName)
        )

        elem.isEmpty mustBe true
      }

      "ensure tell us you moved links are different go to MovedAddressController if flag is true" in new ViewFixture {

        val responsiblePeopleModelWithApprovalCheck = responsiblePeopleModel.copy(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true))
        )

        def view =
          answersView(
            cyaHelper.getHeadingsAndSummaryLists(
              responsiblePeopleModelWithApprovalCheck,
              businessMatching,
              personName.fullName,
              1,
              None,
              true,
              true
            ),
            1,
            true,
            personName.fullName,
            None
          )

        val resultUrl = controllers.responsiblepeople.address.routes.MovedAddressController.get(1).url

        doc.getElementById("rpaddress-isUK-edit").attr("href") mustBe resultUrl
        doc.getElementById("rpaddress-edit").attr("href") mustBe resultUrl
      }

      "ensure tell us you moved links are different if flag is false" in new ViewFixture {

        val responsiblePeopleModelWithApprovalCheck = responsiblePeopleModel.copy(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true))
        )

        def view =
          answersView(
            cyaHelper.getHeadingsAndSummaryLists(
              responsiblePeopleModelWithApprovalCheck,
              businessMatching,
              personName.fullName,
              1,
              None,
              false,
              true
            ),
            1,
            true,
            personName.fullName,
            None
          )

        doc
          .getElementById("rpaddress-isUK-edit")
          .attr("href") mustBe controllers.responsiblepeople.address.routes.CurrentAddressController
          .get(1, true, None)
          .url
        doc
          .getElementById("rpaddress-edit")
          .attr("href") mustBe controllers.responsiblepeople.address.routes.CurrentAddressUKController
          .get(1, true, None)
          .url
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
    "James",
    Some("Michael"),
    "Smith"
  )

  val residenceType = PersonResidenceType(
    UKResidence(Nino(nino)),
    Some(Country("United Kingdom", "GB")),
    Some(Country("United Kingdom", "GB"))
  )

  val nonUKResidenceType = PersonResidenceType(
    NonUKResidence,
    Some(Country("Spain", "ES")),
    Some(Country("United States", "US"))
  )

  val personAddress1 = PersonAddressUK(
    "addressLine1",
    Some("addressLine2"),
    Some("addressLine3"),
    Some("addressLine4"),
    "postCode1"
  )
  val personAddress2 = PersonAddressUK(
    "addressLine5",
    Some("addressLine6"),
    Some("addressLine7"),
    Some("addressLine8"),
    "postCode2"
  )
  val personAddress3 = PersonAddressUK(
    "addressLine9",
    Some("addressLine10"),
    Some("addressLine11"),
    Some("addressLine12"),
    "postCode3"
  )

  val nonUKAddress1 = PersonAddressNonUK(
    "6277 Brookmere Road",
    Some("Small Town"),
    Some("Big County"),
    Some("Washington D.C"),
    Country("United States", "US")
  )

  val nonUKAddress2 = PersonAddressNonUK(
    "The Cottage",
    Some("Sleepy Village"),
    Some("Country Retreat"),
    Some("Farming Area"),
    Country("France", "FR")
  )

  val nonUKAddress3 = PersonAddressNonUK(
    "51 Apartment Block",
    Some("Suburbia"),
    Some("District 4"),
    Some("Capital City"),
    Country("Spain", "ES")
  )

  val currentAddress = ResponsiblePersonCurrentAddress(
    personAddress = personAddress1,
    timeAtAddress = Some(ZeroToFiveMonths),
    dateOfChange = Some(DateOfChange(LocalDate.of(1990, 2, 24)))
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

  val currentAddressNonUK = ResponsiblePersonCurrentAddress(
    personAddress = nonUKAddress1,
    timeAtAddress = Some(ZeroToFiveMonths),
    dateOfChange = Some(DateOfChange(LocalDate.of(1990, 2, 24)))
  )

  val additionalAddressNonUK = ResponsiblePersonAddress(
    personAddress = nonUKAddress2,
    timeAtAddress = Some(SixToElevenMonths)
  )

  val additionalExtraAddressNonUK = ResponsiblePersonAddress(
    personAddress = nonUKAddress3,
    timeAtAddress = Some(OneToThreeYears)
  )

  val addressHistoryNonUK = ResponsiblePersonAddressHistory(
    currentAddress = Some(currentAddressNonUK),
    additionalAddress = Some(additionalAddressNonUK),
    additionalExtraAddress = Some(additionalExtraAddressNonUK)
  )

  val positions = Positions(
    positions = Set(BeneficialOwner, NominatedOfficer),
    startDate = Some(PositionStartDate(LocalDate.of(1990, 2, 24)))
  )

  val responsiblePeopleModel = ResponsiblePerson(
    personName = Some(personName),
    personResidenceType = Some(residenceType),
    contactDetails = Some(ContactDetails("0142980012013", "e@mail.com")),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    vatRegistered = Some(VATRegisteredYes("9876543210")),
    saRegistered = Some(SaRegisteredYes("12345678912")),
    experienceTraining = Some(ExperienceTrainingYes("experience")),
    training = Some(TrainingYes("training")),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))
  )
}
