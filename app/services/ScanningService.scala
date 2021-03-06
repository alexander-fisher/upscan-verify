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

package services

import javax.inject.Inject

import model.S3ObjectLocation
import uk.gov.hmrc.clamav._
import uk.gov.hmrc.clamav.model.{Clean, Infected}

import scala.concurrent.{ExecutionContext, Future}

sealed trait ScanningResult extends Product with Serializable {
  def location: S3ObjectLocation
}

case class FileIsClean(location: S3ObjectLocation) extends ScanningResult

case class FileIsInfected(location: S3ObjectLocation, details: String) extends ScanningResult

trait ScanningService {
  def scan(location: S3ObjectLocation): Future[ScanningResult]
}

class ClamAvScanningService @Inject()(clamClientFactory: ClamAntiVirusFactory, fileManager: FileManager)(
  implicit ec: ExecutionContext)
    extends ScanningService {

  override def scan(location: S3ObjectLocation): Future[ScanningResult] =
    for {
      fileBytes <- fileManager.getBytes(location)
      antivirusClient = clamClientFactory.getClient()
      scanResult <- antivirusClient.sendAndCheck(fileBytes) map {
                     case Clean             => FileIsClean(location)
                     case Infected(message) => FileIsInfected(location, message)
                   }
    } yield {
      scanResult
    }
}
