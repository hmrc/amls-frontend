/*
 * Copyright 2019 HM Revenue & Customs
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

package services.cache

//import config.AppConfig
//import models.responsiblepeople.{ResponsiblePeopleValues, ResponsiblePerson}
//import org.joda.time.{DateTime, DateTimeZone}
//import org.mockito.Matchers._
//import org.mockito.Mockito._
//import org.scalatest.concurrent.ScalaFutures
//import play.api.libs.json.Json
//import reactivemongo.api.DefaultDB
//import uk.gov.hmrc.crypto.ApplicationCrypto
//import uk.gov.hmrc.http.cache.client.CacheMap
//import utils.AmlsSpec
//import play.api.test.Helpers._
//
//import scala.concurrent.Future
//
//class MongoCacheClientSpec extends AmlsSpec with ScalaFutures {
//
//  trait Fixture extends ResponsiblePeopleValues {
//    val mockConfig = mock[AppConfig]
//    val mockDb = mock[() => DefaultDB]
//    val mockApplicationCrypto = mock[ApplicationCrypto]
//    val emptyCacheMap = CacheMap("", Map.empty)
//
//    val mongoClient = new MongoCacheClient(mockConfig, mockDb, mockApplicationCrypto)
//
//    when(mockConfig.mongoEncryptionEnabled).thenReturn(false)
//
//    val testModel = completeModelUkResident
//    val cacheMap = CacheMap("", Map("" -> Json.toJson(testModel)))
//    val cache: Cache = Cache("", Map("" -> Json.toJson(testModel)), DateTime.now(DateTimeZone.UTC))
//  }
//
//  "createOrUpdateWithCacheMiss" must {
//    "return cache for oid" ignore new Fixture {
//    val oldId = "oldId"
//      val newId = "newId"
//      val key = "firstName"
//      val data: String = "{\\\"_id\\\":\\\"oldId\\\",\\\"data\\\":{\\\"add-person\\\":{\\\"firstName\\\":\\\"Jim\\\",\\\"middleName\\\":null,\\\"lastName\\\":\\\"Bin\\\",\\\"roleWithinBusiness\\\":[\\\"Director\\\",\\\"NominatedOfficer\\\"]},\\\"trading-premises\\\":[{\\\"yourTradingPremises\\\":{\\\"tradingName\\\":\\\"Alex Fergu\\\",\\\"addressLine1\\\":\\\"23 High Street\\\",\\\"addressLine4\\\":\\\"Gloucestershire\\\",\\\"postcode\\\":\\\"NE98 1ZZ\\\",\\\"addressLine3\\\":\\\"Gloucester\\\",\\\"addressLine2\\\":\\\"Park View\\\",\\\"isResidential\\\":true,\\\"startDate\\\":\\\"2000-01-01\\\"},\\\"whatDoesYourBusinessDoAtThisAddress\\\":{\\\"activities\\\":[\\\"02\\\"]},\\\"hasChanged\\\":true,\\\"hasAccepted\\\":true}],\\\"responsible-people\\\":[{\\\"personName\\\":{\\\"firstName\\\":\\\"Jim\\\",\\\"lastName\\\":\\\"Bin\\\"},\\\"legalName\\\":{\\\"hasPreviousName\\\":false},\\\"knownBy\\\":{\\\"hasOtherNames\\\":false},\\\"personResidenceType\\\":{\\\"nino\\\":\\\"JS123456A\\\",\\\"countryOfBirth\\\":\\\"GB\\\",\\\"nationality\\\":\\\"GB\\\"},\\\"dateOfBirth\\\":{\\\"dateOfBirth\\\":\\\"1980-01-01\\\"},\\\"contactDetails\\\":{\\\"phoneNumber\\\":\\\"02012345678\\\",\\\"emailAddress\\\":\\\"test@m.c\\\"},\\\"addressHistory\\\":{\\\"currentAddress\\\":{\\\"personAddress\\\":{\\\"personAddressLine1\\\":\\\"23 High Street\\\",\\\"personAddressLine2\\\":\\\"Park View\\\",\\\"personAddressLine3\\\":\\\"Gloucester\\\",\\\"personAddressLine4\\\":\\\"Gloucestershire\\\",\\\"personAddressPostCode\\\":\\\"NE98 1ZZ\\\"},\\\"timeAtAddress\\\":{\\\"timeAtAddress\\\":\\\"04\\\"}}},\\\"positions\\\":{\\\"positions\\\":[\\\"02\\\",\\\"04\\\"],\\\"startDate\\\":\\\"2000-01-01\\\"},\\\"saRegistered\\\":{\\\"saRegistered\\\":false},\\\"experienceTraining\\\":{\\\"experienceTraining\\\":false},\\\"training\\\":{\\\"training\\\":false},\\\"approvalFlags\\\":{\\\"hasAlreadyPassedFitAndProper\\\":true,\\\"hasAlreadyPaidApprovalCheck\\\":true},\\\"hasChanged\\\":true,\\\"hasAccepted\\\":true,\\\"soleProprietorOfAnotherBusiness\\\":{\\\"soleProprietorOfAnotherBusiness\\\":false}}],\\\"business-matching\\\":{\\\"businessName\\\":\\\"Alex Fergu\\\",\\\"businessType\\\":\\\"Corporate Body\\\",\\\"businessAddress\\\":{\\\"line_1\\\":\\\"23 High Street\\\",\\\"line_2\\\":\\\"Park View\\\",\\\"line_3\\\":\\\"Gloucester\\\",\\\"line_4\\\":\\\"Gloucestershire\\\",\\\"postcode\\\":\\\"NE98 1ZZ\\\",\\\"country\\\":\\\"GB\\\"},\\\"safeId\\\":\\\"XE0001234567890\\\",\\\"utr\\\":\\\"1111111111\\\",\\\"businessActivities\\\":[\\\"02\\\"],\\\"companyRegistrationNumber\\\":\\\"12345678\\\",\\\"hasChanged\\\":true,\\\"hasAccepted\\\":true,\\\"preAppComplete\\\":true},\\\"data-import\\\":{\\\"filename\\\":\\\"LTDmissingBusinessDetails\\\"},\\\"business-activities\\\":{\\\"involvedInOther\\\":false,\\\"expectedAMLSTurnover\\\":\\\"03\\\",\\\"businessFranchise\\\":false,\\\"isRecorded\\\":false,\\\"ncaRegistered\\\":false,\\\"accountantForAMLSRegulations\\\":false,\\\"hasWrittenGuidance\\\":false,\\\"hasPolicy\\\":false,\\\"employeeCount\\\":\\\"3\\\",\\\"employeeCountAMLSSupervision\\\":\\\"2\\\",\\\"hasChanged\\\":true,\\\"hasAccepted\\\":true},\\\"bank-details\\\":[],\\\"about-the-business\\\":{\\\"previouslyRegistered\\\":{\\\"previouslyRegistered\\\":true,\\\"prevMLRRegNo\\\":\\\"12345678\\\"},\\\"vatRegistered\\\":{\\\"registeredForVAT\\\":true,\\\"vrnNumber\\\":\\\"123456789\\\"},\\\"corporationTaxRegistered\\\":{\\\"registeredForCorporationTax\\\":true,\\\"corporationTaxReference\\\":\\\"1111111111\\\"},\\\"contactingYou\\\":{\\\"phoneNumber\\\":\\\"+44 (0)123 456-7890\\\",\\\"email\\\":\\\"test@test.com\\\"},\\\"registeredOffice\\\":{\\\"addressLine1\\\":\\\"23 High Street\\\",\\\"addressLine2\\\":\\\"Park View\\\",\\\"addressLine3\\\":\\\"Gloucester\\\",\\\"addressLine4\\\":\\\"Gloucestershire\\\",\\\"postCode\\\":\\\"NE98 1ZZ\\\",\\\"dateOfChange\\\":null},\\\"altCorrespondenceAddress\\\":false,\\\"hasChanged\\\":true,\\\"hasAccepted\\\":true}},\\\"id\\\":\\\"4088688865613872\\\",\\\"lastUpdated\\\":\\\"2019-06-12T09:32:26.779Z\\\"}"
//
//      whenReady (mongoClient.createOrUpdateWithCacheMiss[String](oldId, newId, data, key)(any())) {
//        _ mustEqual cacheMap
//      }
//
//    }
//
//    "return cache for credId" in {
//
//    }
//  }
//
//  "removeByKey" must {
//    "return updated cache when removing" in {
//
//    }
//  }
//
//  "upsert" must {
//    "return CacheMap when inserting" in {
//
//    }
//  }
//
//  "find" must {
//    "return queried item" in new Fixture {
//      when(mongoClient.fetchAll(any()))
//        .thenReturn(Future.successful(Option(cache)))
//
//      val result = mongoClient.find[ResponsiblePerson]("", "")(any())
//      await(result) mustBe Some(testModel)
//    }
//  }
//
//  "findWithCacheMiss" must {
//    "return cache for old Id" in {
//
//    }
//
//    "return cache for cred Id" in {
//
//    }
//  }
//
//  "fetchAll" must {
//    "return whole cache" in {
//
//    }
//  }
//
//  "fetchAllWithDefault" must {
//    "fetch whole cache and return defaults where is no data" in {
//
//    }
//  }
//
//  "removeById" must {
//    "return updated cache when removing by Id" in {
//
//    }
//  }
//
//  "saveAll" must {
//    "return true if data was succesfully saved" in {
//
//    }
//
//    "return false if save failed" in {
//
//    }
//  }
//
//  "createIndex" must {
//    "return true if index created succesfully" in {
//
//    }
//
//    "return false if failed to create index" in {
//
//    }
//  }
//
//  "handleWriteResult" must {
//    "return true result is ok" in {
//
//    }
//
//    "return false/throw an error if there are errors" in {
//
//    }
//  }
//
//  "tryDecrypt" must {
//    "return plain text" in {
//
//    }
//  }
//
//}
