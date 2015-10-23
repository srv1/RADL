/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.gradle


/**
 * Gradle extension for RADL specific properties.
 */
class RadlExtension {

  def coreVersion = '1.0.22'
  def dirName = 'src/main/radl'
  def serviceName
  def packagePrefix
  def docsDir = 'docs/rest'
  def extraSourceDir
  def extraClasspath
  def skipClasspath = false
  def extraProcessors
  def keepArgumentsFile = false
  def scm = 'default'
  def header = 'Generated by RADL.'
  def serializeModel = false

  def preExtracts = []
  def preExtract(clos) {
    preExtracts += clos
  }

  def generateDirName
  def generateSpring = false
  def springVersion = '4.2.1.RELEASE'

}
