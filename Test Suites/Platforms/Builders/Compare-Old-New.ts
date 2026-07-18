<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Captura pareada de ~21 modulos cubriendo los 7 BCs (BC-01 Brand, BC-02 Layouts, BC-03 Templates, BC-04 Task Creation, BC-08 Layout Generation, BC-09 Initiatives/Blueprint, BC-10 File Delivery): app vieja (builder.templet.io) vs app nueva (testing-templet-builder-saas.vercel.app). Genera vieja-XX y nueva-XX .png en Reports/Builders/Compare/. Luego correr Scripts/compare_screenshots.py para el diff visual + COMPARISON-REPORT.html.</description>
   <name>Compare-Old-New</name>
   <tag>Compare, Builders, Screenshots</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>
   <testCaseLink>
      <guid>builders-compare-tc-001</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/compare-old-new-app</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
