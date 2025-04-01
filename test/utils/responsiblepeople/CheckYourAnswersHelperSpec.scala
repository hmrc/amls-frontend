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

package utils.responsiblepeople

import controllers.responsiblepeople.NinoUtil
import models.businessmatching.BusinessActivity.AccountancyServices
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.{Country, DateOfChange}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.govukfrontend.views.Aliases
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{AmlsSpec, DateHelper}

import java.time.LocalDate

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  trait RowFixture extends ResponsiblePeopleValues {

    val headingsAndSummaryLists: Seq[(String, SummaryList)]
    def headings: Seq[String]                = headingsAndSummaryLists.map(_._1)
    def summaryListRows: Seq[SummaryListRow] = headingsAndSummaryLists.flatMap(_._2.rows)

    def assertSubheadingMatches(messageKey: String, sectionIndex: Int): Assertion =
      assert(headings.lift(sectionIndex).contains(messages(messageKey)))

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }

    def toBulletList[A](coll: Seq[A]): String =
      "<ul class=\"govuk-list govuk-list--bullet\">" +
        coll.map { x =>
          s"<li>$x</li>"
        }.mkString +
        "</ul>"

    def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
      messages("lbl.yes")
    } else {
      messages("lbl.no")
    }

    def addressToLines(addressLines: Seq[String]): String =
      "<ul class=\"govuk-list\">" +
        addressLines.map { line =>
          s"""<li>$line<li>"""
        }.mkString + "</ul>"
  }

  ".createSummaryList" when {

    "Personal Details section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.1", 0)
      }
    }

    "Person Name is present" must {

      "render the correct row" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertRowMatches(
          0,
          "responsiblepeople.personName.cya",
          personName.fullName,
          controllers.responsiblepeople.routes.PersonNameController.get(1, true, None).url,
          "rp-personname-edit"
        )
      }
    }

    "Person Name is present" must {

      "render the correct rows" when {

        "person has a previous legal name" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            1,
            messages("responsiblepeople.legalName.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.LegalNameController.get(1, true, None).url,
            "rp-legalname-yes-no-edit"
          )

          assertRowMatches(
            2,
            messages("responsiblepeople.legalNameInput.cya", personName.titleName),
            previousName.fullName,
            controllers.responsiblepeople.routes.LegalNameInputController.get(1, true, None).url,
            "rp-legalname-edit"
          )

          assertRowMatches(
            3,
            messages("responsiblepeople.legalnamechangedate.cya", personName.titleName),
            DateHelper.formatDate(legalNameChangeDate),
            controllers.responsiblepeople.routes.LegalNameChangeDateController.get(1, true, None).url,
            "rp-legalnamechangedate-edit"
          )
        }

        "person has no previous legal name" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel
              .copy(legalName = Some(PreviousName(Some(false), None, None, None)), legalNameChangeDate = None),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            1,
            messages("responsiblepeople.legalName.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.LegalNameController.get(1, true, None).url,
            "rp-legalname-yes-no-edit"
          )
        }
      }
    }

    "Known By is present" must {

      "render the correct rows" when {

        "person is known by another name" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            4,
            messages("responsiblepeople.knownby.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.KnownByController.get(1, true, None).url,
            "rp-knownby-edit"
          )

          assertRowMatches(
            5,
            messages("responsiblepeople.knownby.answer", personName.titleName),
            otherName,
            controllers.responsiblepeople.routes.KnownByController.get(1, true, None).url,
            "rp-knownby-true-edit"
          )
        }

        "person has no previous legal name" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(knownBy = Some(KnownBy(Some(false), None))),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            4,
            messages("responsiblepeople.knownby.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.KnownByController.get(1, true, None).url,
            "rp-knownby-edit"
          )
        }
      }
    }

    "Date of Birth is present" must {

      "render the correct row" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.titleName,
          1,
          None,
          true,
          true
        )

        assertRowMatches(
          6,
          messages("responsiblepeople.detailed_answers.dob.cya", personName.titleName),
          DateHelper.formatDate(dateOfBirth),
          controllers.responsiblepeople.routes.DateOfBirthController.get(1, true, None).url,
          "date-of-birth"
        )
      }
    }

    "Place of Birth and Nationality section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.2", 1)
      }
    }

    "Residence is present" must {

      "render the correct rows" when {

        "person has UK residence" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            7,
            messages("responsiblepeople.detailed_answers.uk_resident", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.PersonResidentTypeController.get(1, true, None).url,
            "rp-ukresident-edit"
          )

          assertRowMatches(
            8,
            messages("responsiblepeople.detailed_answers.uk_resident.nino", personName.titleName),
            nino,
            controllers.responsiblepeople.routes.PersonResidentTypeController.get(1, true, None).url,
            "rp-ukresident-true-edit"
          )
        }

        "person has non-UK residence" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(personResidenceType = Some(nonUKResidenceType)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            7,
            messages("responsiblepeople.detailed_answers.uk_resident", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.PersonResidentTypeController.get(1, true, None).url,
            "rp-ukresident-edit"
          )
        }
      }
    }

    "Has UK Passport is present" must {

      "render the correct rows" when {

        "person has UK passport" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            9,
            messages("responsiblepeople.detailed_answers.uk.passport", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.PersonUKPassportController.get(1, true, None).url,
            "uk-passport"
          )

          assertRowMatches(
            10,
            messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName.titleName),
            passportNumber,
            controllers.responsiblepeople.routes.PersonUKPassportController.get(1, true, None).url,
            "uk-passport-true-edit"
          )
        }

        "person does not have a UK passport" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(ukPassport = Some(UKPassportNo)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            9,
            messages("responsiblepeople.detailed_answers.uk.passport", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.PersonUKPassportController.get(1, true, None).url,
            "uk-passport"
          )
        }
      }
    }

    "Has Non-UK Passport is present" must {

      "render the correct rows" when {

        "person has Non-UK passport" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            11,
            messages("responsiblepeople.detailed_answers.non.uk.passport", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, true, None).url,
            "rp-nonukpassport-edit"
          )

          assertRowMatches(
            12,
            messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName.titleName),
            passportNumber,
            controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, true, None).url,
            "rp-nonukpassport-true-edit"
          )
        }

        "person does not have a passport" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(nonUKPassport = Some(NoPassport)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            11,
            messages("responsiblepeople.detailed_answers.non.uk.passport", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, true, None).url,
            "rp-nonukpassport-edit"
          )
        }
      }
    }

    "Country of Birth is present" must {

      "render the correct rows" when {

        "person was born in the UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            13,
            messages("responsiblepeople.country.of.birth.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.CountryOfBirthController.get(1, true, None).url,
            "rp-countryofbirth-edit"
          )

          assertRowMatches(
            14,
            messages("responsiblepeople.detailed_answers.country_of_birth", personName.titleName),
            uk.name,
            controllers.responsiblepeople.routes.CountryOfBirthController.get(1, true, None).url,
            "rp-countryofbirth-answer-edit"
          )
        }

        "person was not born in the UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(personResidenceType = Some(nonUKResidenceType)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            12,
            messages("responsiblepeople.country.of.birth.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.CountryOfBirthController.get(1, true, None).url,
            "rp-countryofbirth-edit"
          )

          assertRowMatches(
            13,
            messages("responsiblepeople.detailed_answers.country_of_birth", personName.titleName),
            spain.name,
            controllers.responsiblepeople.routes.CountryOfBirthController.get(1, true, None).url,
            "rp-countryofbirth-answer-edit"
          )
        }
      }
    }

    "Nationality is present" must {

      "render the correct rows" when {

        "person is a UK citizen" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            15,
            messages("responsiblepeople.nationality.heading", personName.titleName),
            messages("responsiblepeople.nationality.selection.british"),
            controllers.responsiblepeople.routes.NationalityController.get(1, true, None).url,
            "rp-nationality-edit"
          )
        }

        "person is not a UK citizen" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(personResidenceType = Some(nonUKResidenceType)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            14,
            messages("responsiblepeople.nationality.heading", personName.titleName),
            messages("responsiblepeople.nationality.selection.other"),
            controllers.responsiblepeople.routes.NationalityController.get(1, true, None).url,
            "rp-nationality-edit"
          )

          assertRowMatches(
            15,
            messages("responsiblepeople.nationality.selection.other.answer", personName.titleName),
            usa.name,
            controllers.responsiblepeople.routes.NationalityController.get(1, true, None).url,
            "rp-nationality-other-edit"
          )
        }
      }
    }

    "Contact Details section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.3", 2)
      }
    }

    "Contact Details is present" must {

      "render the correct row" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.titleName,
          1,
          None,
          true,
          true
        )

        assertRowMatches(
          16,
          messages("responsiblepeople.contact_details.cya", personName.titleName),
          s"""<p class="govuk-body">${messages("responsiblepeople.detailed_answers.phone_number")}<br>$phoneNumber</p>
            <p class="govuk-body">${messages("responsiblepeople.detailed_answers.email")}<br>$email</p>
          """,
          controllers.responsiblepeople.routes.ContactDetailsController.get(1, true, None).url,
          "rp-contactDetails-edit"
        )
      }
    }

    "Home Addresses section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.4", 3)
      }
    }

    "Current Address is present" must {

      "render the correct rows" when {

        "Current address is UK and showHide is true" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            17,
            messages("responsiblepeople.detailed_answers.address.UK", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.address.routes.MovedAddressController.get(1).url,
            "rpaddress-isUK-edit"
          )

          assertRowMatches(
            18,
            messages("responsiblepeople.detailed_answers.address.cya", personName.titleName),
            addressToLines(currentAddress.personAddress.toLines),
            controllers.responsiblepeople.address.routes.MovedAddressController.get(1).url,
            "rpaddress-edit"
          )

          assertRowMatches(
            19,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${ZeroToFiveMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(1, true, None).url,
            "rp-timeatataddress-edit"
          )
        }

        "Current address is UK and showHide is false" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            false,
            true
          )

          assertRowMatches(
            17,
            messages("responsiblepeople.detailed_answers.address.UK", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.address.routes.CurrentAddressController.get(1, true, None).url,
            "rpaddress-isUK-edit"
          )

          assertRowMatches(
            18,
            messages("responsiblepeople.detailed_answers.address.cya", personName.titleName),
            addressToLines(currentAddress.personAddress.toLines),
            controllers.responsiblepeople.address.routes.CurrentAddressUKController.get(1, true, None).url,
            "rpaddress-edit"
          )

          assertRowMatches(
            19,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${ZeroToFiveMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(1, true, None).url,
            "rp-timeatataddress-edit"
          )
        }

        "Current address is Non-UK and showHide is true" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(
              addressHistory = Some(ResponsiblePersonAddressHistory(currentAddress = Some(currentAddressNonUK)))
            ),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            17,
            messages("responsiblepeople.detailed_answers.address.UK", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.address.routes.MovedAddressController.get(1).url,
            "rpaddress-isUK-edit"
          )

          assertRowMatches(
            18,
            messages("responsiblepeople.detailed_answers.address.cya", personName.titleName),
            addressToLines(currentAddressNonUK.personAddress.toLines),
            controllers.responsiblepeople.address.routes.MovedAddressController.get(1).url,
            "rpaddress-edit"
          )

          assertRowMatches(
            19,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${ZeroToFiveMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(1, true, None).url,
            "rp-timeatataddress-edit"
          )
        }

        "Current address is Non-UK and showHide is false" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(
              addressHistory = Some(ResponsiblePersonAddressHistory(currentAddress = Some(currentAddressNonUK)))
            ),
            businessMatching,
            personName.titleName,
            1,
            None,
            false,
            true
          )

          assertRowMatches(
            17,
            messages("responsiblepeople.detailed_answers.address.UK", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.address.routes.CurrentAddressController.get(1, true, None).url,
            "rpaddress-isUK-edit"
          )

          assertRowMatches(
            18,
            messages("responsiblepeople.detailed_answers.address.cya", personName.titleName),
            addressToLines(currentAddressNonUK.personAddress.toLines),
            controllers.responsiblepeople.address.routes.CurrentAddressNonUKController.get(1, true, None).url,
            "rpaddress-edit"
          )

          assertRowMatches(
            19,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${ZeroToFiveMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(1, true, None).url,
            "rp-timeatataddress-edit"
          )
        }
      }
    }

    "Additional Address is present" must {

      "render the correct rows" when {

        "Additional address is UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            20,
            messages("responsiblepeople.detailed_answers.address.previous.UK", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.address.routes.AdditionalAddressController.get(1, true, None).url,
            "rp-previousaddress-isUK-edit"
          )

          assertRowMatches(
            21,
            messages("responsiblepeople.detailed_answers.address.previous.cya", personName.titleName),
            addressToLines(additionalAddress.personAddress.toLines),
            controllers.responsiblepeople.address.routes.AdditionalAddressUKController.get(1, true, None).url,
            "rp-previousaddress-edit"
          )

          assertRowMatches(
            22,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${SixToElevenMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtAdditionalAddressController.get(1, true, None).url,
            "rp-timeatatpreviousaddress-edit"
          )
        }

        "Additional address is Non-UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(
              addressHistory = Some(
                ResponsiblePersonAddressHistory(
                  currentAddress = Some(currentAddress),
                  additionalAddress = Some(additionalAddressNonUK)
                )
              )
            ),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            20,
            messages("responsiblepeople.detailed_answers.address.previous.UK", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.address.routes.AdditionalAddressController.get(1, true, None).url,
            "rp-previousaddress-isUK-edit"
          )

          assertRowMatches(
            21,
            messages("responsiblepeople.detailed_answers.address.previous.cya", personName.titleName),
            addressToLines(additionalAddressNonUK.personAddress.toLines),
            controllers.responsiblepeople.address.routes.AdditionalAddressNonUKController.get(1, true, None).url,
            "rp-previousaddress-edit"
          )

          assertRowMatches(
            22,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${SixToElevenMonths.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtAdditionalAddressController.get(1, true, None).url,
            "rp-timeatatpreviousaddress-edit"
          )
        }
      }
    }

    "Extra Address is present" must {

      "render the correct rows" when {

        "Extra address is UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            23,
            messages("responsiblepeople.detailed_answers.address.other.previous.UK", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.address.routes.AdditionalExtraAddressController.get(1, true, None).url,
            "rp-otherpreviousaddress-isUK-edit"
          )

          assertRowMatches(
            24,
            messages("responsiblepeople.detailed_answers.address.other.previous.cya", personName.titleName),
            addressToLines(additionalExtraAddress.personAddress.toLines),
            controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(1, true, None).url,
            "rp-otherpreviousaddress-edit"
          )

          assertRowMatches(
            25,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${OneToThreeYears.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtAdditionalExtraAddressController.get(1, true, None).url,
            "rp-timeatotherpreviousaddress-edit"
          )
        }

        "Extra address is Non-UK" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(
              addressHistory = Some(
                ResponsiblePersonAddressHistory(
                  currentAddress = Some(currentAddress),
                  additionalAddress = Some(additionalAddressNonUK),
                  additionalExtraAddress = Some(additionalExtraAddressNonUK)
                )
              )
            ),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            23,
            messages("responsiblepeople.detailed_answers.address.other.previous.UK", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.address.routes.AdditionalExtraAddressController.get(1, true, None).url,
            "rp-otherpreviousaddress-isUK-edit"
          )

          assertRowMatches(
            24,
            messages("responsiblepeople.detailed_answers.address.other.previous.cya", personName.titleName),
            addressToLines(additionalExtraAddressNonUK.personAddress.toLines),
            controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(1, true, None).url,
            "rp-otherpreviousaddress-edit"
          )

          assertRowMatches(
            25,
            messages("responsiblepeople.timeataddress.address_history.cya", personName.titleName),
            messages(s"responsiblepeople.timeataddress.${OneToThreeYears.toString}"),
            controllers.responsiblepeople.address.routes.TimeAtAdditionalExtraAddressController.get(1, true, None).url,
            "rp-timeatotherpreviousaddress-edit"
          )
        }
      }
    }

    "Business Details section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.5", 4)
      }
    }

    "Positions is present" must {

      "render the correct rows" when {

        "multiple positions are present" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            26,
            messages("responsiblepeople.position_within_business.cya", personName.titleName),
            toBulletList(
              Set(BeneficialOwner, NominatedOfficer).map(p => PositionWithinBusiness.getPrettyName(p)).toList.sorted
            ),
            controllers.responsiblepeople.routes.PositionWithinBusinessController.get(1, true, None).url,
            "rp-positionwithinbusiness-edit"
          )

          assertRowMatches(
            27,
            messages("responsiblepeople.position_within_business.startDate.cya", personName.titleName),
            DateHelper.formatDate(positionStartDate),
            controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController.get(1, true, None).url,
            "rp-positionstartdate-edit"
          )
        }

        "one position is present" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(
              positions = Some(Positions(Set(BeneficialOwner), Some(PositionStartDate(positionStartDate))))
            ),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            26,
            messages("responsiblepeople.position_within_business.cya", personName.titleName),
            PositionWithinBusiness.getPrettyName(BeneficialOwner),
            controllers.responsiblepeople.routes.PositionWithinBusinessController.get(1, true, None).url,
            "rp-positionwithinbusiness-edit"
          )

          assertRowMatches(
            27,
            messages("responsiblepeople.position_within_business.startDate.cya", personName.titleName),
            DateHelper.formatDate(positionStartDate),
            controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController.get(1, true, None).url,
            "rp-positionstartdate-edit"
          )
        }
      }
    }

    "Sole Proprietor is present" must {

      "render the correct row" when {

        "person is a sole proprietor" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            28,
            messages("responsiblepeople.sole.proprietor.another.business.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(1, true, None).url,
            "rp-soleproprietor-edit"
          )
        }

        "person is not a sole proprietor" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false))),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            28,
            messages("responsiblepeople.sole.proprietor.another.business.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(1, true, None).url,
            "rp-soleproprietor-edit"
          )
        }
      }
    }

    "VAT Registered is present" must {

      "render the correct row" when {

        "person has a VAT number" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            29,
            messages("responsiblepeople.registeredforvat.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.VATRegisteredController.get(1, true, None).url,
            "rp-registeredforvat-edit"
          )

          assertRowMatches(
            30,
            messages("responsiblepeople.detailed_answers.registered_for_vat"),
            vatNumber,
            controllers.responsiblepeople.routes.VATRegisteredController.get(1, true, None).url,
            "rp-registeredforvat-answer-edit"
          )
        }

        "person does not have a VAT number" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(vatRegistered = Some(VATRegisteredNo)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            29,
            messages("responsiblepeople.registeredforvat.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.VATRegisteredController.get(1, true, None).url,
            "rp-registeredforvat-edit"
          )
        }
      }
    }

    "SA Registered is present" must {

      "render the correct row" when {

        "person has a UTR number" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            31,
            messages("responsiblepeople.registeredforselfassessment.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1, true, None).url,
            "rp-registeredforsa-edit"
          )

          assertRowMatches(
            32,
            messages("responsiblepeople.detailed_answers.registered_for_sa.cya"),
            utrNumber,
            controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1, true, None).url,
            "rp-registeredforsa-answer-edit"
          )
        }

        "person does not have a UTR number" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(saRegistered = Some(SaRegisteredNo)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            31,
            messages("responsiblepeople.registeredforselfassessment.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(1, true, None).url,
            "rp-registeredforsa-edit"
          )
        }
      }
    }

    "Experience section is rendered" must {

      "render the correct title" in new RowFixture {
        override val headingsAndSummaryLists: Seq[(String, Aliases.SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
          responsiblePeopleModel,
          businessMatching,
          personName.fullName,
          1,
          None,
          true,
          true
        )

        assertSubheadingMatches("responsiblepeople.check_your_answers.subheading.6", 5)
      }
    }

    "Experience Training is present" must {

      "render the correct row" when {

        "person has had experience training with multiple business activities" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            33,
            messages("responsiblepeople.experiencetraining.heading.multiple", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-edit"
          )

          assertRowMatches(
            34,
            messages("responsiblepeople.detailed_answers.previous_experience.detail.cya", personName.titleName),
            experienceDescription,
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-answer-edit"
          )
        }

        "person has had experience training with a single business activity" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching.copy(
              activities = Some(BusinessActivities(Set(AccountancyServices)))
            ),
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            33,
            messages(
              "responsiblepeople.experiencetraining.heading",
              personName.titleName,
              businessMatching.prefixedAlphabeticalBusinessTypes(true).fold("")(names => names.head)
            ),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-edit"
          )

          assertRowMatches(
            34,
            messages("responsiblepeople.detailed_answers.previous_experience.detail.cya", personName.titleName),
            experienceDescription,
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-answer-edit"
          )
        }

        "person has no experience training with multiple business activities" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(experienceTraining = Some(ExperienceTrainingNo)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            33,
            messages("responsiblepeople.experiencetraining.heading.multiple", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-edit"
          )
        }

        "person has no experience training with a single business activity" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(experienceTraining = Some(ExperienceTrainingNo)),
            businessMatching.copy(
              activities = Some(BusinessActivities(Set(AccountancyServices)))
            ),
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            33,
            messages(
              "responsiblepeople.experiencetraining.heading",
              personName.titleName,
              businessMatching.prefixedAlphabeticalBusinessTypes(true).fold("")(names => names.head)
            ),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.ExperienceTrainingController.get(1, true, None).url,
            "rp-training-edit"
          )
        }
      }
    }

    "Training is present" must {

      "render the correct row" when {

        "person has had training" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            35,
            messages("responsiblepeople.training.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.TrainingController.get(1, true, None).url,
            "rp-traininginmlre-edit"
          )

          assertRowMatches(
            36,
            messages("responsiblepeople.detailed_answers.training_in_anti_money_laundering", personName.titleName),
            trainingDescription,
            controllers.responsiblepeople.routes.TrainingController.get(1, true, None).url,
            "rp-traininginmlre-answer-edit"
          )
        }

        "person has no experience training with multiple business activities" in new RowFixture {
          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(training = Some(TrainingNo)),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            35,
            messages("responsiblepeople.training.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.TrainingController.get(1, true, None).url,
            "rp-traininginmlre-edit"
          )
        }
      }
    }

    "Passed Fit And Proper Assessment is present" must {

      "render the correct row" when {

        "person has passed" in new RowFixture {

          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            37,
            messages("responsiblepeople.fit_and_proper.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.FitAndProperController.get(1, true, None).url,
            "fit-and-proper"
          )
        }

        "person has not passed" in new RowFixture {

          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false))),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            37,
            messages("responsiblepeople.fit_and_proper.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.FitAndProperController.get(1, true, None).url,
            "fit-and-proper"
          )
        }
      }
    }

    "Has Already Paid For Approval Check is present" must {

      "render the correct row" when {

        "person has paid" in new RowFixture {

          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel,
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            38,
            messages("responsiblepeople.approval_check.heading", personName.titleName),
            booleanToLabel(true),
            controllers.responsiblepeople.routes.ApprovalCheckController.get(1, true, None).url,
            "approval-check"
          )
        }

        "person has not paid" in new RowFixture {

          override val headingsAndSummaryLists: Seq[(String, SummaryList)] = cyaHelper.getHeadingsAndSummaryLists(
            responsiblePeopleModel.copy(approvalFlags = ApprovalFlags(Some(true), Some(false))),
            businessMatching,
            personName.titleName,
            1,
            None,
            true,
            true
          )

          assertRowMatches(
            38,
            messages("responsiblepeople.approval_check.heading", personName.titleName),
            booleanToLabel(false),
            controllers.responsiblepeople.routes.ApprovalCheckController.get(1, true, None).url,
            "approval-check"
          )
        }
      }
    }
  }
}

trait ResponsiblePeopleValues extends NinoUtil {

  val nino: String = nextNino

  val previousName: PreviousName = PreviousName(
    Some(true),
    Some("firstName"),
    Some("middleName"),
    Some("lastName")
  )

  val legalNameChangeDate: LocalDate = LocalDate.now().minusYears(1)

  val personName: PersonName = PersonName(
    "James",
    Some("Michael"),
    "Smith"
  )

  val otherName: String = "James Mark Jones"

  val dateOfBirth: LocalDate = LocalDate.of(1990, 3, 14)

  val passportNumber: String = "123456789"

  val uk: Country    = Country("United Kingdom", "GB")
  val spain: Country = Country("Spain", "ES")
  val usa: Country   = Country("United States", "US")

  val residenceType: PersonResidenceType = PersonResidenceType(
    UKResidence(Nino(nino)),
    Some(uk),
    Some(uk)
  )

  val nonUKResidenceType: PersonResidenceType = PersonResidenceType(
    NonUKResidence,
    Some(spain),
    Some(usa)
  )

  val phoneNumber: String = "0142980012013"
  val email: String       = "e@mail.com"

  val personAddress1: PersonAddressUK = PersonAddressUK(
    "addressLine1",
    Some("addressLine2"),
    Some("addressLine3"),
    Some("addressLine4"),
    "postCode1"
  )
  val personAddress2: PersonAddressUK = PersonAddressUK(
    "addressLine5",
    Some("addressLine6"),
    Some("addressLine7"),
    Some("addressLine8"),
    "postCode2"
  )
  val personAddress3: PersonAddressUK = PersonAddressUK(
    "addressLine9",
    Some("addressLine10"),
    Some("addressLine11"),
    Some("addressLine12"),
    "postCode3"
  )

  val nonUKAddress1: PersonAddressNonUK = PersonAddressNonUK(
    "6277 Brookmere Road",
    Some("Small Town"),
    Some("Big County"),
    Some("Washington D.C"),
    Country("United States", "US")
  )

  val nonUKAddress2: PersonAddressNonUK = PersonAddressNonUK(
    "The Cottage",
    Some("Sleepy Village"),
    Some("Country Retreat"),
    Some("Farming Area"),
    Country("France", "FR")
  )

  val nonUKAddress3: PersonAddressNonUK = PersonAddressNonUK(
    "51 Apartment Block",
    Some("Suburbia"),
    Some("District 4"),
    Some("Capital City"),
    Country("Spain", "ES")
  )

  val currentAddress: ResponsiblePersonCurrentAddress = ResponsiblePersonCurrentAddress(
    personAddress = personAddress1,
    timeAtAddress = Some(ZeroToFiveMonths),
    dateOfChange = Some(DateOfChange(LocalDate.of(1990, 2, 24)))
  )

  val additionalAddress: ResponsiblePersonAddress = ResponsiblePersonAddress(
    personAddress = personAddress2,
    timeAtAddress = Some(SixToElevenMonths)
  )

  val additionalExtraAddress: ResponsiblePersonAddress = ResponsiblePersonAddress(
    personAddress = personAddress3,
    timeAtAddress = Some(OneToThreeYears)
  )

  val addressHistory: ResponsiblePersonAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(currentAddress),
    additionalAddress = Some(additionalAddress),
    additionalExtraAddress = Some(additionalExtraAddress)
  )

  val currentAddressNonUK: ResponsiblePersonCurrentAddress = ResponsiblePersonCurrentAddress(
    personAddress = nonUKAddress1,
    timeAtAddress = Some(ZeroToFiveMonths),
    dateOfChange = Some(DateOfChange(LocalDate.of(1990, 2, 24)))
  )

  val additionalAddressNonUK: ResponsiblePersonAddress = ResponsiblePersonAddress(
    personAddress = nonUKAddress2,
    timeAtAddress = Some(SixToElevenMonths)
  )

  val additionalExtraAddressNonUK: ResponsiblePersonAddress = ResponsiblePersonAddress(
    personAddress = nonUKAddress3,
    timeAtAddress = Some(OneToThreeYears)
  )

  val addressHistoryNonUK: ResponsiblePersonAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(currentAddressNonUK),
    additionalAddress = Some(additionalAddressNonUK),
    additionalExtraAddress = Some(additionalExtraAddressNonUK)
  )

  val positionStartDate: LocalDate = LocalDate.of(1990, 2, 24)

  val positions: Positions = Positions(
    positions = Set(BeneficialOwner, NominatedOfficer),
    startDate = Some(PositionStartDate(positionStartDate))
  )

  val vatNumber: String = "9876543210"

  val utrNumber: String = "12345678912"

  val experienceDescription: String = "I have great experience in this"
  val trainingDescription: String   = "I have been trained in this"

  val responsiblePeopleModel: ResponsiblePerson = ResponsiblePerson(
    personName = Some(personName),
    legalName = Some(previousName),
    legalNameChangeDate = Some(legalNameChangeDate),
    knownBy = Some(KnownBy(Some(true), Some(otherName))),
    dateOfBirth = Some(DateOfBirth(dateOfBirth)),
    personResidenceType = Some(residenceType),
    ukPassport = Some(UKPassportYes(passportNumber)),
    nonUKPassport = Some(NonUKPassportYes(passportNumber)),
    contactDetails = Some(ContactDetails(phoneNumber, email)),
    addressHistory = Some(addressHistory),
    positions = Some(positions),
    vatRegistered = Some(VATRegisteredYes(vatNumber)),
    saRegistered = Some(SaRegisteredYes(utrNumber)),
    experienceTraining = Some(ExperienceTrainingYes(experienceDescription)),
    training = Some(TrainingYes(trainingDescription)),
    approvalFlags = ApprovalFlags(Some(true), Some(true)),
    soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(true))
  )

  val businessMatching = BusinessMatching(activities = Some(BusinessActivities(BusinessActivities.all)))
}
