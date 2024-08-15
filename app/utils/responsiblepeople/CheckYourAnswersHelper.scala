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

import models.businessmatching.BusinessMatching
import models.responsiblepeople._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList}
import utils.{CheckYourAnswersHelperFunctions, DateHelper}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends CheckYourAnswersHelperFunctions {

  def getHeadingsAndSummaryLists(model: ResponsiblePerson,
                                 businessMatching: BusinessMatching,
                                 personName: String,
                                 idx: Int,
                                 flow: Option[String],
                                 showHide: Boolean = false,
                                 showApprovalSection: Boolean = false
                                )(implicit messages: Messages): Seq[(String, SummaryList)] = {

    Seq(
      (
        messages("responsiblepeople.check_your_answers.subheading.1"),
        SummaryList(
          getPersonNameRow(model, idx, flow).asSeq ++
          getLegalNameRows(model, personName, idx, flow).asSeq ++
          getKnownByRows(model, personName, idx, flow).asSeq ++
          getDateOfBirthRow(model, personName, idx, flow),
          classes = "govuk-!-margin-bottom-9"
        )
      ),
      (
        messages("responsiblepeople.check_your_answers.subheading.2"),
        SummaryList(
          getPersonResidenceRows(model, personName, idx, flow).asSeq ++
          getUKPassportRows(model, personName, idx, flow).asSeq ++
          getNonUKPassportRows(model, personName, idx, flow).asSeq ++
          getCountryAndNationalityRows(model, personName, idx, flow).asSeq,
          classes = "govuk-!-margin-bottom-9"
        )
      ),
      (
        messages("responsiblepeople.check_your_answers.subheading.3"),
        SummaryList(
          getContactDetailsRow(model, personName, idx, flow).asSeq,
          classes = "govuk-!-margin-bottom-9"
        )
      ),
      (
        messages("responsiblepeople.check_your_answers.subheading.4"),
        SummaryList(
          getCurrentAddressRows(model, personName, idx, flow, showHide).asSeq ++
          getAdditionalAddressRows(model, personName, idx, flow).asSeq ++
          getExtraAddressRows(model, personName, idx, flow).asSeq,
          classes = "govuk-!-margin-bottom-9"
        )
      ),
      (
        messages("responsiblepeople.check_your_answers.subheading.5"),
        SummaryList(
          getPositionsRows(model, personName, idx, flow).asSeq ++
          getSoleProprietorRow(model, personName, idx, flow).asSeq ++
          getVATRegisteredRows(model, personName, idx, flow).asSeq ++
          getSARegisteredRows(model, personName, idx, flow).asSeq,
          classes = "govuk-!-margin-bottom-9"
        )
      ),
      (
        messages("responsiblepeople.check_your_answers.subheading.6"),
        SummaryList(
          getExperienceTrainingRows(model, businessMatching, personName, idx, flow).asSeq ++
          getTrainingRows(model, personName, idx, flow).asSeq ++
          getApprovalFlagsRow(model, personName, idx, flow).asSeq ++
          (if (showApprovalSection) {
            getHasAlreadyPaidRow(model, personName, idx, flow)
          } else {
            None
          }).asSeq,
          classes = "govuk-!-margin-bottom-9"
        )
      )
    )
  }

  private def getPersonNameRow(model: ResponsiblePerson, idx: Int, flow: Option[String])(implicit messages: Messages): Option[SummaryListRow] = {
    model.personName.map { name =>
      row(
        "responsiblepeople.personName.title",
        name.fullName,
        editAction(
          controllers.responsiblepeople.routes.PersonNameController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.responsiblePersonName",
          "rp-personname-edit"
        )
      )
    }
  }

  private def getLegalNameRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.legalName.map { name =>

      val dateRow = model.legalNameChangeDate.map { date =>
        row(
          messages("responsiblepeople.legalnamechangedate.heading", personName),
          DateHelper.formatDate(date),
          editAction(
            controllers.responsiblepeople.routes.LegalNameChangeDateController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.whenNameChanged",
            "rp-legalnamechangedate-edit"
          )
        )
      }

      def legalNameRow(boolean: Boolean): SummaryListRow = row(
        messages("responsiblepeople.legalName.heading", personName),
        booleanToLabel(boolean),
        editAction(
          controllers.responsiblepeople.routes.LegalNameController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.personChangedName",
          "rp-legalname-yes-no-edit"
        )
      )

      (name.hasPreviousName, dateRow) match {
        case (Some(true), Some(dateRow)) => Seq(
          legalNameRow(true),
          row(
            messages("responsiblepeople.legalNameInput.heading", personName),
            name.fullName,
            editAction(
              controllers.responsiblepeople.routes.LegalNameInputController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.personsPreviousName",
              "rp-legalname-edit"
            )
          ),
          dateRow
        )
        case (Some(false), Some(dateRow)) => Seq(legalNameRow(false), dateRow)
        case _ => Seq(legalNameRow(false))
      }
    }
  }

  private def getKnownByRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.knownBy.map { knownBy =>

      def knownByRow(boolean: Boolean): SummaryListRow = row(
        messages("responsiblepeople.knownby.heading", personName),
        booleanToLabel(boolean),
        editAction(
          controllers.responsiblepeople.routes.KnownByController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.knownByOtherNames",
          "rp-knownby-edit"
        )
      )

      if (knownBy.hasOtherNames.contains(true)) {
        Seq(
          knownByRow(true),
          row(
            messages("responsiblepeople.knownby.answer", personName),
            knownBy.otherName,
            editAction(
              controllers.responsiblepeople.routes.KnownByController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.otherNamesKnownBy",
              "rp-knownby-true-edit"
            )
          )
        )
      } else {
        Seq(knownByRow(false))
      }
    }
  }

  private def getDateOfBirthRow(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[SummaryListRow] = {
    model.dateOfBirth.map { dob =>
      row(
        messages("responsiblepeople.detailed_answers.dob", personName),
        DateHelper.formatDate(dob.dateOfBirth),
        editAction(
          controllers.responsiblepeople.routes.DateOfBirthController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.personsDoB",
          "date-of-birth"
        )
      )
    }
  }

  private def getPersonResidenceRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.personResidenceType.map { residenceType =>

      def personResidenceRow(boolean: Boolean): SummaryListRow = row(
        messages("responsiblepeople.detailed_answers.uk_resident", personName),
        booleanToLabel(boolean),
        editAction(
          controllers.responsiblepeople.routes.PersonResidentTypeController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.personUKRes",
          "rp-ukresident-edit"
        )
      )

      residenceType.isUKResidence match {
        case UKResidence(nino) =>
          Seq(
            personResidenceRow(true),
            row(
              messages("responsiblepeople.detailed_answers.uk_resident.nino", personName),
              nino.value,
              editAction(
                controllers.responsiblepeople.routes.PersonResidentTypeController.get(idx, true, flow).url,
                "responsiblepeople.checkYourAnswers.change.personsNINo",
                "rp-ukresident-true-edit"
              )
            )
          )
        case NonUKResidence => Seq(personResidenceRow(false))
      }
    }
  }

  private def getUKPassportRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.ukPassport.map { passport =>

      def ukPassportRow(boolean: Boolean): SummaryListRow = row(
        messages("responsiblepeople.detailed_answers.uk.passport", personName),
        booleanToLabel(boolean),
        editAction(
          controllers.responsiblepeople.routes.PersonUKPassportController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.hasUKPassport",
          "uk-passport"
        )
      )

      passport match {
        case UKPassportYes(number) =>
          Seq(
            ukPassportRow(true),
            row(
              messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName),
              number,
              editAction(
                controllers.responsiblepeople.routes.PersonUKPassportController.get(idx, true, flow).url,
                "responsiblepeople.checkYourAnswers.change.personsPassportNo",
                "uk-passport-true-edit"
              )
            )
          )
        case UKPassportNo => Seq(ukPassportRow(false))
      }
    }
  }

  private def getNonUKPassportRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.nonUKPassport.map { passport =>

      def nonUKPassportRow(boolean: Boolean): SummaryListRow = row(
        messages("responsiblepeople.detailed_answers.non.uk.passport", personName),
        booleanToLabel(boolean),
        editAction(
          controllers.responsiblepeople.routes.PersonNonUKPassportController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.passportFrAnotherCountry",
          "rp-nonukpassport-edit"
        )
      )

      passport match {
        case NonUKPassportYes(number) =>
          Seq(
            nonUKPassportRow(true),
            row(
              messages("responsiblepeople.detailed_answers.uk_resident.passport_number", personName),
              number,
              editAction(
                controllers.responsiblepeople.routes.PersonNonUKPassportController.get(idx, true, flow).url,
                "responsiblepeople.checkYourAnswers.change.anotherCountryPassportNumber",
                "rp-nonukpassport-true-edit"
              )
            )
          )
        case NoPassport => Seq(nonUKPassportRow(false))
      }
    }
  }

  private def getCountryAndNationalityRows(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.personResidenceType.flatMap { residenceType =>

      def isBritishRow(boolean: Boolean) = row(
        messages("responsiblepeople.nationality.heading", personName),
        if (boolean) {
          messages("responsiblepeople.nationality.selection.british")
        } else {
          messages("responsiblepeople.nationality.selection.other")
        },
        editAction(
          controllers.responsiblepeople.routes.NationalityController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.personsNationality",
          "rp-nationality-edit"
        )
      )

      residenceType.countryOfBirth.map { country =>
        Seq(
          row(
            messages("responsiblepeople.country.of.birth.heading", personName),
            if (country.isUK) booleanToLabel(true) else booleanToLabel(false),
            editAction(
              controllers.responsiblepeople.routes.CountryOfBirthController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.bornInUK",
              "rp-countryofbirth-edit"
            )
          ),
          row(
            messages("responsiblepeople.detailed_answers.country_of_birth", personName),
            country.name,
            editAction(
              controllers.responsiblepeople.routes.CountryOfBirthController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.countryOfBirth",
              "rp-countryofbirth-answer-edit"
            )
          )
        ) ++ (
          if(country.isUK) {
            Seq(isBritishRow(true))
          } else {
            Seq(
              isBritishRow(false),
              row(
                messages("responsiblepeople.nationality.selection.other.answer", personName),
                residenceType.nationality.fold("")(_.name),
                editAction(
                  controllers.responsiblepeople.routes.NationalityController.get(idx, true, flow).url,
                  "responsiblepeople.checkYourAnswers.change.countryNationalTo",
                  "rp-nationality-other-edit"
                )
              )
            )
          }
        )
      }
    }
  }

  private def getContactDetailsRow(model: ResponsiblePerson, personName: String, idx: Int, flow: Option[String])(implicit messages: Messages): Option[SummaryListRow] = {
    model.contactDetails.map { details =>
      SummaryListRow(
        Key(Text(messages("responsiblepeople.contact_details.heading", personName))),
        Value(HtmlContent(
          s"""<p class="govuk-body">${messages("responsiblepeople.detailed_answers.phone_number")}<br>${details.phoneNumber}</p>
            <p class="govuk-body">${messages("responsiblepeople.detailed_answers.email")}<br>${details.emailAddress}</p>
          """)),
        actions = editAction(
          controllers.responsiblepeople.routes.ContactDetailsController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.contactDetails",
          "rp-contactDetails-edit"
        )
      )
    }
  }

  private def getCurrentAddressRows(model: ResponsiblePerson,
                                    personName: String,
                                    idx: Int,
                                    flow: Option[String],
                                    showHide: Boolean
                                   )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.addressHistory.flatMap(_.currentAddress).map { currentAddress =>
      Seq(
        row(
          messages("responsiblepeople.detailed_answers.address.UK", personName),
          currentAddress.personAddress.isUK(),
          editAction(
            if(showHide) {
              controllers.responsiblepeople.address.routes.MovedAddressController.get(idx).url
            } else {
              controllers.responsiblepeople.address.routes.CurrentAddressController.get(idx, true, flow).url
            },
            "responsiblepeople.checkYourAnswers.change.homeAddressUK",
            "rpaddress-isUK-edit"
          )
        ),
        SummaryListRow(
          Key(Text(messages("responsiblepeople.detailed_answers.address", personName))),
          addressToLines(currentAddress.personAddress.toLines),
          actions = editAction(
            (currentAddress.personAddress, showHide) match {
              case (_, true) =>
                controllers.responsiblepeople.address.routes.MovedAddressController.get(idx).url
              case (_: PersonAddressNonUK, false) =>
                controllers.responsiblepeople.address.routes.CurrentAddressNonUKController.get(idx, true, flow).url
              case (_: PersonAddressUK, false) =>
                controllers.responsiblepeople.address.routes.CurrentAddressUKController.get(idx, true, flow).url
            },
            "responsiblepeople.checkYourAnswers.change.homeAddress",
            "rpaddress-edit"
          )
        ),
        row(
          messages("responsiblepeople.timeataddress.address_history.heading", personName),
          currentAddress.timeAtAddress.fold("")(x => messages(s"responsiblepeople.timeataddress.${x.toString}")),
          editAction(
            controllers.responsiblepeople.address.routes.TimeAtCurrentAddressController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.addressDuration",
            "rp-timeatataddress-edit"
          )
        )
      )
    }
  }

  private def getAdditionalAddressRows(model: ResponsiblePerson,
                                       personName: String,
                                       idx: Int,
                                       flow: Option[String]
                                      )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.addressHistory.flatMap(_.additionalAddress).map { additionalAddress =>
      Seq(
        row(
          messages("responsiblepeople.detailed_answers.address.previous.UK", personName),
          additionalAddress.personAddress.isUK(),
          editAction(
            controllers.responsiblepeople.address.routes.AdditionalAddressController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.previousAddressUK",
            "rp-previousaddress-isUK-edit"
          )
        ),
        SummaryListRow(
          Key(Text(messages("responsiblepeople.detailed_answers.address.previous", personName))),
          addressToLines(additionalAddress.personAddress.toLines),
          actions = editAction(
            additionalAddress.personAddress match {
              case _: PersonAddressNonUK =>
                controllers.responsiblepeople.address.routes.AdditionalAddressNonUKController.get(idx, true, flow).url
              case _: PersonAddressUK =>
                controllers.responsiblepeople.address.routes.AdditionalAddressUKController.get(idx, true, flow).url
            },
            "responsiblepeople.checkYourAnswers.change.previousAddress",
            "rp-previousaddress-edit"
          )
        ),
        row(
          messages("responsiblepeople.timeataddress.address_history.heading", personName),
          additionalAddress.timeAtAddress.fold("")(x => messages(s"responsiblepeople.timeataddress.${x.toString}")),
          editAction(
            controllers.responsiblepeople.address.routes.TimeAtAdditionalAddressController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.previousAddressDuration",
            "rp-timeatatpreviousaddress-edit"
          )
        )
      )
    }
  }

  private def getExtraAddressRows(model: ResponsiblePerson,
                                  personName: String,
                                  idx: Int,
                                  flow: Option[String]
                                 )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.addressHistory.flatMap(_.additionalExtraAddress).map { extraAddress =>
      Seq(
        row(
          messages("responsiblepeople.detailed_answers.address.other.previous.UK", personName),
          extraAddress.personAddress.isUK(),
          editAction(
            controllers.responsiblepeople.address.routes.AdditionalExtraAddressController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.previousAddressUK",
            "rp-otherpreviousaddress-isUK-edit"
          )
        ),
        SummaryListRow(
          Key(Text(messages("responsiblepeople.detailed_answers.address.other.previous", personName))),
          addressToLines(extraAddress.personAddress.toLines),
          actions = editAction(
            extraAddress.personAddress match {
              case _: PersonAddressNonUK =>
                controllers.responsiblepeople.address.routes.AdditionalExtraAddressNonUKController.get(idx, true, flow).url
              case _: PersonAddressUK =>
                controllers.responsiblepeople.address.routes.AdditionalExtraAddressUKController.get(idx, true, flow).url
            },
            "responsiblepeople.checkYourAnswers.change.previousAddress",
            "rp-otherpreviousaddress-edit"
          )
        ),
        row(
          messages("responsiblepeople.timeataddress.address_history.heading", personName),
          extraAddress.timeAtAddress.fold("")(x => messages(s"responsiblepeople.timeataddress.${x.toString}")),
          editAction(
            controllers.responsiblepeople.address.routes.TimeAtAdditionalExtraAddressController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.previousAddressDuration",
            "rp-timeatotherpreviousaddress-edit"
          )
        )
      )
    }
  }

  private def getPositionsRows(model: ResponsiblePerson,
                               personName: String,
                               idx: Int,
                               flow: Option[String]
                              )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.positions.map { pos =>
      Seq(
        SummaryListRow(
          Key(Text(messages("responsiblepeople.position_within_business.heading", personName))),
          if(pos.positions.size == 1) {
            Value(Text(PositionWithinBusiness.getPrettyName(pos.positions.head)))
          } else {
            toBulletList(pos.positions.map(p => PositionWithinBusiness.getPrettyName(p)).toList.sorted)
          },
          actions = editAction(
            controllers.responsiblepeople.routes.PositionWithinBusinessController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.role",
            "rp-positionwithinbusiness-edit"
          )
        ),
        row(
          messages("responsiblepeople.position_within_business.startDate.heading", personName),
          pos.startDate.map(sd => DateHelper.formatDate(sd.startDate)).getOrElse(""),
          editAction(
            controllers.responsiblepeople.routes.PositionWithinBusinessStartDateController.get(idx, true, flow).url,
            "responsiblepeople.checkYourAnswers.change.roleStarted",
            "rp-positionstartdate-edit"
          )
        )
      )
    }
  }

  private def getSoleProprietorRow(model: ResponsiblePerson,
                                   personName: String,
                                   idx: Int,
                                   flow: Option[String]
                                  )(implicit messages: Messages): Option[SummaryListRow] = {

    model.soleProprietorOfAnotherBusiness.map { anotherBusiness =>
      row(
        messages("responsiblepeople.sole.proprietor.another.business.heading", personName),
        booleanToLabel(anotherBusiness.soleProprietorOfAnotherBusiness),
        editAction(
          controllers.responsiblepeople.routes.SoleProprietorOfAnotherBusinessController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.soleProprietorAnotherBus",
          "rp-soleproprietor-edit"
        )
      )
    }
  }

  private def getVATRegisteredRows(model: ResponsiblePerson,
                                   personName: String,
                                   idx: Int,
                                   flow: Option[String]
                                  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def vatYesNoRow(isRegistered: Boolean): SummaryListRow = {
      row(
        messages("responsiblepeople.registeredforvat.heading", personName),
        booleanToLabel(isRegistered),
        editAction(
          controllers.responsiblepeople.routes.VATRegisteredController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.registeredVAT",
          "rp-registeredforvat-edit"
        )
      )
    }

    model.vatRegistered map {
      case VATRegisteredYes(regNumber) =>
        Seq(
          vatYesNoRow(true),
          row(
            messages("responsiblepeople.detailed_answers.registered_for_vat"),
            regNumber,
            editAction(
              controllers.responsiblepeople.routes.VATRegisteredController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.VATRegistrationNo",
              "rp-registeredforvat-answer-edit"
            )
          )
        )
      case VATRegisteredNo => Seq(vatYesNoRow(false))
    }
  }

  private def getSARegisteredRows(model: ResponsiblePerson,
                                   personName: String,
                                   idx: Int,
                                   flow: Option[String]
                                  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def saYesNoRow(isRegistered: Boolean): SummaryListRow = {
      row(
        messages("responsiblepeople.registeredforselfassessment.heading", personName),
        booleanToLabel(isRegistered),
        editAction(
          controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.selfAssessmentRegistered",
          "rp-registeredforsa-edit"
        )
      )
    }

    model.saRegistered map {
      case SaRegisteredYes(regNumber) =>
        Seq(
          saYesNoRow(true),
          row(
            messages("responsiblepeople.detailed_answers.registered_for_sa"),
            regNumber,
            editAction(
              controllers.responsiblepeople.routes.RegisteredForSelfAssessmentController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.UTR",
              "rp-registeredforsa-answer-edit"
            )
          )
        )
      case SaRegisteredNo => Seq(saYesNoRow(false))
    }
  }

  private def getExperienceTrainingRows(model: ResponsiblePerson,
                                        businessMatching: BusinessMatching,
                                        personName: String,
                                        idx: Int,
                                        flow: Option[String]
                                       )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def trainingYesNoRow(hasTraining: Boolean): SummaryListRow = {
      row(
        businessMatching.activities.fold(messages("responsiblepeople.experiencetraining.title")) { types =>
          if (types.businessActivities.size > 1) {
            messages("responsiblepeople.experiencetraining.heading.multiple", personName)
          } else {
            messages(
              "responsiblepeople.experiencetraining.heading",
              personName,
              businessMatching.prefixedAlphabeticalBusinessTypes(true).fold("")(names => names.head)
            )
          }
        },
        booleanToLabel(hasTraining),
        editAction(
          controllers.responsiblepeople.routes.ExperienceTrainingController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.previousExperience",
          "rp-training-edit"
        )
      )
    }

    model.experienceTraining map {
      case ExperienceTrainingYes(info) =>
        Seq(
          trainingYesNoRow(true),
          row(
            messages("responsiblepeople.detailed_answers.previous_experience.detail", personName),
            info,
            editAction(
              controllers.responsiblepeople.routes.ExperienceTrainingController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.previousExperienceDesc",
              "rp-training-answer-edit"
            )
          )
        )
      case ExperienceTrainingNo => Seq(trainingYesNoRow(false))
    }
  }

  private def getTrainingRows(model: ResponsiblePerson,
                              personName: String,
                              idx: Int,
                              flow: Option[String]
                             )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def trainingYesNoRow(hasTraining: Boolean): SummaryListRow = {
      row(
        messages("responsiblepeople.training.heading", personName),
        booleanToLabel(hasTraining),
        editAction(
          controllers.responsiblepeople.routes.TrainingController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.AMLTraining",
          "rp-traininginmlre-edit"
        )
      )
    }

    model.training map {
      case TrainingYes(info) =>
        Seq(
          trainingYesNoRow(true),
          row(
            messages("responsiblepeople.detailed_answers.training_in_anti_money_laundering", personName),
            info,
            editAction(
              controllers.responsiblepeople.routes.TrainingController.get(idx, true, flow).url,
              "responsiblepeople.checkYourAnswers.change.trainingDesc",
              "rp-traininginmlre-answer-edit"
            )
          )
        )
      case TrainingNo => Seq(trainingYesNoRow(false))
    }
  }

  private def getApprovalFlagsRow(model: ResponsiblePerson,
                                  personName: String,
                                  idx: Int,
                                  flow: Option[String]
                                 )(implicit messages: Messages): Option[SummaryListRow] = {

    model.approvalFlags.hasAlreadyPassedFitAndProper.map { fp =>
      row(
        messages("responsiblepeople.fit_and_proper.heading", personName),
        booleanToLabel(fp),
        editAction(
          controllers.responsiblepeople.routes.FitAndProperController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.fitProperPass",
          "fit-and-proper"
        )
      )
    }
  }

  private def getHasAlreadyPaidRow(model: ResponsiblePerson,
                                  personName: String,
                                  idx: Int,
                                  flow: Option[String]
                                 )(implicit messages: Messages): Option[SummaryListRow] = {

    model.approvalFlags.hasAlreadyPaidApprovalCheck.map { fp =>
      row(
        messages("responsiblepeople.approval_check.heading", personName),
        booleanToLabel(fp),
        editAction(
          controllers.responsiblepeople.routes.ApprovalCheckController.get(idx, true, flow).url,
          "responsiblepeople.checkYourAnswers.change.HMRCChargedApproval",
          "approval-check"
        )
      )
    }
  }

  implicit class OptionUnwrapper[A](opt: Option[A]) {
    def asSeq: Seq[SummaryListRow] = opt.fold(Seq.empty[SummaryListRow]){
      case x: Seq[SummaryListRow] => x
      case x: SummaryListRow => Seq(x)
    }
  }
}
