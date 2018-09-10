/*
 * Copyright 2018 HM Revenue & Customs
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

import java.math.BigInteger
import java.security.MessageDigest

import org.scalatest.MustMatchers
import utils.{AmlsSpec, AuthorisedFixture}

import scala.io.Source

class NotificationsCheckSumSpec extends AmlsSpec with MustMatchers {

    trait NotificationsCheckSumFixture extends AuthorisedFixture {
        val versionNumbers: Seq[String] = Seq(
            "v1m0"
        )
        val checkSumRoute: String = "./conf/notifications/"
        def generateCheckSum(s: String): String =
            String.format("%032x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(s.getBytes)))
    }

    trait ServicesRouteFixture extends NotificationsCheckSumFixture {
        val route: String = "./app/services/notifications/"
        val suffix: String = ".scala"
        val files: Seq[String] = Seq(
            "MessageDetails"
        )
        val checkSumName: String = "services.txt"
    }

    trait ViewsRouteFixture extends NotificationsCheckSumFixture {
        val route: String = "./app/views/notifications/"
        val suffix: String = ".scala.html"
        val files: Seq[String] = Seq(
            "message_details",
            "minded_to_reject",
            "minded_to_revoke",
            "no_longer_minded_to_reject",
            "no_longer_minded_to_revoke",
            "rejection_reasons",
            "revocation_reasons"
        )
        val checkSumName: String = "views.txt"
    }

    "Checksums must be equal - services" in new ServicesRouteFixture {
        versionNumbers.foreach(versionNumber => {
            val checkSumSource = Source.fromFile(s"${ checkSumRoute }${ versionNumber }/${ checkSumName }")
            val checkSums: Map[String, String] = checkSumSource.getLines().map(line => {
                val kv = line.split("=")
                s"${ versionNumber }/${ kv(0) }" -> kv(1)
            }).toMap
            checkSumSource.close()
            files.foreach(fileName => {
                val source = Source.fromFile(s"${ route }${ versionNumber }/${ fileName }${ suffix }")
                val lines: String = try source.mkString finally source.close()
                val checkSum: String = generateCheckSum(lines)
                assert(checkSum == checkSums(s"${ versionNumber }/${ fileName }"),
                    s"Replace checksum for ${ versionNumber }/${ fileName } with ${ checkSum }")
            })
        })
    }

    "Checksums must be equal - views" in new ViewsRouteFixture {
        versionNumbers.foreach(versionNumber => {
            val checkSumSource = Source.fromFile(s"${ checkSumRoute }${ versionNumber }/${ checkSumName }")
            val checkSums: Map[String, String] = checkSumSource.getLines().map(line => {
                val kv = line.split("=")
                s"${ versionNumber }/${ kv(0) }" -> kv(1)
            }).toMap
            checkSumSource.close()
            files.foreach(fileName => {
                val source = Source.fromFile(s"${ route }${ versionNumber }/${ fileName }${ suffix }")
                val lines: String = try source.mkString finally source.close()
                val checkSum: String = generateCheckSum(lines)
                assert(checkSum == checkSums(s"${ versionNumber }/${ fileName }"),
                    s"Replace checksum for ${ versionNumber }/${ fileName } with ${ checkSum }")
            })
        })
    }
}
