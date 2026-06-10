<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>SUPER SUITE: valida todo el proyecto en una sola corrida. Bloques por plataforma con isReuseDriver (1 login por bloque): Sheets, Decks, Email (objects + filters), Builders (objects + smoke + redirects), Builders Tracking (4 tabs, email filters Blueprint), Schedulers. Usar para regresión completa pre-commit.</description>
   <name>Super-Suite-Validation</name>
   <tag>Super,Full,CrossPlatform,Tracking,Regression</tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>false</rerunImmediately>

   <!-- ===== SHEETS (1 login) ===== -->
   <testCaseLink>
      <guid>super-sheets-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-sheets-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-sheets-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-sheets-04</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/filters/client-initiative-sort</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-sheets-05</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Sheets/filters/initiative-content-validation</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== DECKS (1 login) ===== -->
   <testCaseLink>
      <guid>super-decks-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-decks-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-decks-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-decks-04</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/filters/initiative-content-validation</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-decks-05</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Decks/filters/client-initiative-sort</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== EMAIL (1 login) ===== -->
   <testCaseLink>
      <guid>super-email-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-email-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/list-view-visible-clickable</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-email-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/objects/list-actions-click-response</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-email-04</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/filters/client-initiative-sort</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-email-05</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Email/filters/initiative-content-validation</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== BUILDERS - smoke público (sin login, browser propio) ===== -->
   <testCaseLink>
      <guid>super-builders-smoke-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/smoke</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-builders-smoke-02</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/functional-smoke</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== BUILDERS - objects + redirects (1 login) ===== -->
   <testCaseLink>
      <guid>super-builders-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-builders-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/objects/click-visible-objects-return-home</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-builders-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/direct-redirect/all-options</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== BUILDERS - TRACKING (1 login, email filters en Blueprint) ===== -->
   <testCaseLink>
      <guid>super-tracking-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-all-dashboard</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-tracking-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-blueprint-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-tracking-03</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-task-creation-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-tracking-04</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Builders/tracking/validate-login-tab</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

   <!-- ===== SCHEDULERS ===== -->
   <testCaseLink>
      <guid>super-schedulers-smoke-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/smoke</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-schedulers-smoke-02</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/functional-smoke</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-schedulers-01</guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/objects/validate-objects-visible</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
   <testCaseLink>
      <guid>super-schedulers-02</guid>
      <isReuseDriver>true</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId>Test Cases/Schedulers/objects/click-visible-objects-return-home</testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>

</TestSuiteEntity>
