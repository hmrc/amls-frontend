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

package utils

import org.scalatest.MustMatchers
import jto.validation.{Path, Write}
import org.scalatest.WordSpec


class MappingUtilsSpec extends WordSpec with MustMatchers{
  "spm" must {
    "generate a writes that can write Sequences" in {
      case class TestData(data : String)

      val testDataPMWrites  =  Write { td:TestData =>
        Map((Path \ "TestDataPath") -> s"TESTDATA : ${td.data}")
      }

      val SUT = MappingUtils.spm[TestData](testDataPMWrites)


      SUT.writes(
        Seq(
          TestData("AAAA"),
          TestData("BBBB"),
          TestData("CCCC"),
          TestData("DDDD"),
          TestData("EEEE"),
          TestData("FFFF"),
          TestData("GGGG"),
          TestData("HHHH")
        )
      ) must be (
        Map (
          (Path \ 0 \ "TestDataPath" ) -> "TESTDATA : AAAA",
          (Path \ 1 \ "TestDataPath" ) -> "TESTDATA : BBBB",
          (Path \ 2 \ "TestDataPath" ) -> "TESTDATA : CCCC",
          (Path \ 3 \ "TestDataPath" ) -> "TESTDATA : DDDD",
          (Path \ 4 \ "TestDataPath" ) -> "TESTDATA : EEEE",
          (Path \ 5 \ "TestDataPath" ) -> "TESTDATA : FFFF",
          (Path \ 6 \ "TestDataPath" ) -> "TESTDATA : GGGG",
          (Path \ 7 \ "TestDataPath" ) -> "TESTDATA : HHHH"
        )
      )
    }

    "Allow duplicates in the written sequences" in {
      case class TestData(data : String)

      val testDataPMWrites  =  Write { td:TestData =>
        Map((Path \ "TestDataPath") -> s"TESTDATA : ${td.data}")
      }

      val SUT = MappingUtils.spm[TestData](testDataPMWrites)


      SUT.writes(
        Seq(
          TestData("AAAA"),
          TestData("BBBB"),
          TestData("CCCC"),
          TestData("DDDD"),
          TestData("DDDD"),
          TestData("DDDD"),
          TestData("GGGG"),
          TestData("HHHH")
        )
      ) must be (
        Map (
          (Path \ 0 \ "TestDataPath" ) -> "TESTDATA : AAAA",
          (Path \ 1 \ "TestDataPath" ) -> "TESTDATA : BBBB",
          (Path \ 2 \ "TestDataPath" ) -> "TESTDATA : CCCC",
          (Path \ 3 \ "TestDataPath" ) -> "TESTDATA : DDDD",
          (Path \ 4 \ "TestDataPath" ) -> "TESTDATA : DDDD",
          (Path \ 5 \ "TestDataPath" ) -> "TESTDATA : DDDD",
          (Path \ 6 \ "TestDataPath" ) -> "TESTDATA : GGGG",
          (Path \ 7 \ "TestDataPath" ) -> "TESTDATA : HHHH"
        )
      )
    }
  }
}
