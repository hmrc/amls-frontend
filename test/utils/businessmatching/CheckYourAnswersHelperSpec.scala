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

package utils.businessmatching

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching.{BusinessActivities, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes, BusinessMatching, BusinessMatchingMsbServices, BusinessType, CompanyRegistrationNumber, TypeOfBusiness}
import play.api.test.FakeRequest
import play.test.Helpers.fakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.AmlsSpec

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val businessAddressIndex = 0
  val registrationTypeIndex = 1
  val registeredServicesIndex = 2
  val msbActivitiesIndex = 3
  val hasPsrNumberIndex = 4
  val psrNumberIndex = 5

  def failIfEmpty = fail("HTML was not present")

  val businessAppliedForPSRNumberModel = BusinessAppliedForPSRNumberYes("123456")

  ".createSummaryList" must {

    val msbServices = BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, ForeignExchange))
    val businessActivitiesModel = BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
    val businessAddress = Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AB1 2CD"), Country("United Kingdom", "GB"))
    val reviewDetailsModel = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany), businessAddress, "XE0000000000000")
    val typeOfBusinessModel = TypeOfBusiness("Charity")
    val companyRegistrationNumberModel = CompanyRegistrationNumber("12345678")

    def checkChangeLink(slr: SummaryListRow, href: String, id: String) = {
      val changeLink = slr.actions.getOrElse(failIfEmpty).items.headOption.getOrElse(failIfEmpty)

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }

    "return the correct summary list row" which {

      "contains Business Address" when {

        "review details are present" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(reviewDetails = Some(reviewDetailsModel)), isPreSubmission = true, isPending = false
          ).rows.lift(businessAddressIndex).getOrElse(failIfEmpty)

          result.key.toString must include(messages("businessmatching.summary.business.address.lbl"))

          businessAddress.toLines.foreach { line =>
            result.value.toString must include(line)
          }

          result.actions mustBe None
        }
      }

      "contains Business Registration Number" when {

        "Business Type is LPrLLP" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(
                reviewDetailsModel.copy(businessType = Some(BusinessType.LPrLLP))
              ),
              companyRegistrationNumber = Some(companyRegistrationNumberModel)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(registrationTypeIndex).getOrElse(failIfEmpty)

          result.key.toString must include(messages("businessmatching.registrationnumber.title"))

          result.value.toString must include(companyRegistrationNumberModel.companyRegistrationNumber)
        }

        "Business Type is LimitedCompany" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel),
              companyRegistrationNumber = Some(companyRegistrationNumberModel)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(registrationTypeIndex).getOrElse(failIfEmpty)

          result.key.toString must include(messages("businessmatching.registrationnumber.title"))

          result.value.toString must include(companyRegistrationNumberModel.companyRegistrationNumber)
        }
      }

      "contains Business Type" when {

        "Business Type is UnincorporatedBody" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(
                reviewDetailsModel.copy(businessType = Some(BusinessType.UnincorporatedBody))
              ),
              typeOfBusiness = Some(typeOfBusinessModel)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(registrationTypeIndex).getOrElse(failIfEmpty)

          result.key.toString must include(messages("businessmatching.typeofbusiness.title"))

          result.value.toString must include(typeOfBusinessModel.typeOfBusiness)
        }
      }

      "contains Business Activities" when {

        val id = "businessactivities-edit"

        "a single Business Activity is present" that {

          val bm = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel),
            companyRegistrationNumber = Some(companyRegistrationNumberModel),
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          "has the correct change link when is in pre-submission but not pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = true,
              isPending = false
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${HighValueDealing.value}"))

            checkChangeLink(result, controllers.businessmatching.routes.RegisterServicesController.get().url, id)
          }

          "has the correct change link when is NOT in pre-submission and not pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = false,
              isPending = false
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${HighValueDealing.value}"))

            checkChangeLink(result, controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url, id)
          }

          "has no change link when is pending" in {

            val result = cyaHelper.createSummaryList(
              BusinessMatching(
                reviewDetails = Some(reviewDetailsModel),
                companyRegistrationNumber = Some(companyRegistrationNumberModel),
                activities = Some(BusinessActivities(Set(HighValueDealing)))
              ),
              isPreSubmission = false,
              isPending = true
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${HighValueDealing.value}"))

            result.actions mustBe None
          }
        }

        "multiple Business Activities are present" that {

          val bm = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel),
            companyRegistrationNumber = Some(companyRegistrationNumberModel),
            activities = Some(businessActivitiesModel)
          )

          "has the correct change link when is in pre-submission but not pending" in {

            val result = cyaHelper.createSummaryList(
              bm, isPreSubmission = true, isPending = false
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            businessActivitiesModel.businessActivities foreach { activity =>
              result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${activity.value}"))
            }

            checkChangeLink(result, controllers.businessmatching.routes.RegisterServicesController.get().url, id)
          }

          "has the correct change link when is NOT in pre-submission and not pending" in {

            val result = cyaHelper.createSummaryList(
              bm, isPreSubmission = false, isPending = false
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            businessActivitiesModel.businessActivities foreach { activity =>
              result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${activity.value}"))
            }

            checkChangeLink(result, controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url, id)
          }

          "has no change link when is pending" in {

            val result = cyaHelper.createSummaryList(
              bm, isPreSubmission = false, isPending = true
            ).rows.lift(registeredServicesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.registerservices.title"))

            businessActivitiesModel.businessActivities foreach { activity =>
              result.value.toString must include(messages(s"businessmatching.registerservices.servicename.lbl.${activity.value}"))
            }

            result.actions mustBe None
          }
        }
      }

      "contains MSB Activities" when {

        val id = "msbservices-edit"

        "a single activity is present" that {

          val bm = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel),
            companyRegistrationNumber = Some(companyRegistrationNumberModel),
            activities = Some(businessActivitiesModel),
            msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
          )

          "has the correct change link when status is not pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = true,
              isPending = false
            ).rows.lift(msbActivitiesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.services.title"))

            result.value.toString must include(messages(s"businessmatching.services.list.lbl.${TransmittingMoney.value}"))

            checkChangeLink(result, controllers.businessmatching.routes.MsbSubSectorsController.get(true).url, id)
          }

          "has no change link when status is pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = true,
              isPending = true
            ).rows.lift(msbActivitiesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.services.title"))

            result.value.toString must include(messages(s"businessmatching.services.list.lbl.${TransmittingMoney.value}"))

            result.actions mustBe None
          }
        }

        "multiple activities are present" that {

          val bm = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel),
            companyRegistrationNumber = Some(companyRegistrationNumberModel),
            activities = Some(businessActivitiesModel),
            msbServices = Some(msbServices)
          )

          "has the correct change link when status is not pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = true,
              isPending = false
            ).rows.lift(msbActivitiesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.services.title"))

            msbServices.msbServices foreach { msbService =>
              result.value.toString must include(messages(s"businessmatching.services.list.lbl.${msbService.value}"))
            }

            checkChangeLink(result, controllers.businessmatching.routes.MsbSubSectorsController.get(true).url, id)
          }

          "has no change link when status is pending" in {

            val result = cyaHelper.createSummaryList(
              bm,
              isPreSubmission = true,
              isPending = true
            ).rows.lift(msbActivitiesIndex).getOrElse(failIfEmpty)

            result.key.toString must include(messages("businessmatching.services.title"))

            msbServices.msbServices foreach { msbService =>
              result.value.toString must include(messages(s"businessmatching.services.list.lbl.${msbService.value}"))
            }

            result.actions mustBe None
          }
        }
      }

      "contains PSR number" when {
        "the user has answered yes and supplied a PSR number" in {
          val request = addTokenWithSessionParam(FakeRequest())("originalPsrNumber" -> "123456")

          val bm = BusinessMatching(
            reviewDetails = Some(reviewDetailsModel),
            companyRegistrationNumber = Some(companyRegistrationNumberModel),
            activities = Some(businessActivitiesModel),
            msbServices = Some(msbServices),
            businessAppliedForPSRNumber = Some(businessAppliedForPSRNumberModel)
          )

          val rows = cyaHelper.createSummaryList(
            bm,
            isPreSubmission = true,
            isPending = true
          )(messages, request).rows

          val hasPsrNumberRow = rows.lift(hasPsrNumberIndex).getOrElse(failIfEmpty)
          val psrNumberRow = rows.lift(psrNumberIndex).getOrElse(failIfEmpty)

          hasPsrNumberRow.key.toString must include(messages("businessmatching.psr.number.title"))
          hasPsrNumberRow.value.toString must include(messages("lbl.yes"))

          psrNumberRow.key.toString must include(messages("businessmatching.psr.number.cya.title"))
          psrNumberRow.value.toString must include("123456")

          checkChangeLink(psrNumberRow, controllers.businessmatching.routes.PSRNumberController.get(true).url, "edit-psr-number")
        }
      }
    }

    "return an empty SummaryListRow" when {

      "attempting to render Business Address" which {

        "is caused by an empty address" in {

          cyaHelper.createSummaryList(
            BusinessMatching(), isPreSubmission = true, isPending = false
          ).rows.lift(businessAddressIndex) mustBe None
        }
      }

      "attempting to render Business Registration Number" which {

        "is caused by an empty CompanyRegistrationNumber" in {

          cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel)
            ), isPreSubmission = true, isPending = false
          ).rows.lift(registrationTypeIndex) mustBe None
        }
      }

      "attempting to render Business Type" which {

        "is caused by an empty TypeOfBusiness" in {

          cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(
                reviewDetailsModel.copy(businessType = Some(BusinessType.UnincorporatedBody))
              )
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(registrationTypeIndex) mustBe None
        }
      }

      "attempting to render Business Activities" which {

        "is caused by empty Business Activities" in {

          cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel),
              companyRegistrationNumber = Some(companyRegistrationNumberModel)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(registeredServicesIndex) mustBe None
        }
      }

      "attempting to render MSB Business Activities" which {

        "is caused by empty MSB Business Activities" in {

          cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel),
              companyRegistrationNumber = Some(companyRegistrationNumberModel),
              activities = Some(businessActivitiesModel)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows.lift(msbActivitiesIndex) mustBe None
        }
      }

      "attempting to render PSR number" which {

        "is caused by NOT selecting 'Transmitting Money' in MSB services" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel),
              companyRegistrationNumber = Some(companyRegistrationNumberModel),
              activities = Some(businessActivitiesModel),
              msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal)))
            ),
            isPreSubmission = true,
            isPending = false
          ).rows

          result.lift(hasPsrNumberIndex) mustBe None
          result.lift(psrNumberIndex) mustBe None
        }

        "is caused by the user answering 'No'" in {

          val result = cyaHelper.createSummaryList(
            BusinessMatching(
              reviewDetails = Some(reviewDetailsModel),
              companyRegistrationNumber = Some(companyRegistrationNumberModel),
              activities = Some(businessActivitiesModel),
              msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
              businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)
            ),
            isPreSubmission = true,
            isPending = false
          ).rows

          result.lift(hasPsrNumberIndex) mustBe None
          result.lift(psrNumberIndex) mustBe None
        }
      }
    }
  }

  ".getSubmitButton" must {

    "return the logout link" when {

      "application is in pre-submission and 'No' for PSR number is answered" in {

        val result = cyaHelper.getSubmitButton(
          Some(BusinessAppliedForPSRNumberNo), isPreSubmission = true, preAppCompleted = false
        ).getOrElse(failIfEmpty).toString()

        result must include(messages("button.logout"))
        result must include(appConfig.logoutUrl)
      }
    }

    "return submit button" that {

      "has 'Return to about your business' text" when {

        "pre-submission and pre-app are completed" in {

          val result = cyaHelper.getSubmitButton(
            Some(businessAppliedForPSRNumberModel), isPreSubmission = true, preAppCompleted = true
          ).getOrElse(failIfEmpty).toString()

          result must include(messages("businessmatching.summary.noedit.anchortext"))
        }

        "pre-submission is NOT completed" in {

          val result = cyaHelper.getSubmitButton(None, isPreSubmission = false, preAppCompleted = false).getOrElse(failIfEmpty).toString()

          result must include(messages("businessmatching.summary.noedit.anchortext"))
        }
      }

      "has 'Confirm and start application' text" when {

        "pre-submission is completed but and pre-app is NOT completed" in {

          val result = cyaHelper.getSubmitButton(
            Some(businessAppliedForPSRNumberModel), isPreSubmission = true, preAppCompleted = false
          ).getOrElse(failIfEmpty).toString()

          result must include(messages("businessmatching.button.confirm.start"))
        }
      }
    }
  }
}
