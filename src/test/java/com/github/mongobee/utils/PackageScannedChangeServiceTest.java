package com.github.mongobee.utils;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.exception.MongobeeChangeSetException;
import com.github.mongobee.exception.MongobeeException;
import com.github.mongobee.test.changelogs.*;
import junit.framework.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
public class PackageScannedChangeServiceTest {

  @Test
  public void shouldFindChangeLogObjects() throws MongobeeException{
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);
    // when
    List<Object> foundObjects = service.fetchChangeLogs();
    // then
    assertTrue(foundObjects != null && foundObjects.size() > 0);
  }
  
  @Test
  public void shouldFindChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);
    
    // then
    assertTrue(foundMethods != null && foundMethods.size() == 5);
  }

  @Test
  public void shouldFindAnotherChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(AnotherMongobeeTestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 6);
  }


  @Test
  public void shouldFindIsRunAlwaysMethod() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(AnotherMongobeeTestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")){
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
  }

  @Test
  public void shouldCreateEntry() throws MongobeeChangeSetException {
    
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);

    for (Method foundMethod : foundMethods) {
    
      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);
      
      // then
      Assert.assertEquals("testuser", entry.getAuthor());
      Assert.assertEquals(MongobeeTestResource.class.getName(), entry.getChangeLogClass());
      Assert.assertNotNull(entry.getTimestamp());
      Assert.assertNotNull(entry.getChangeId());
      Assert.assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test(expected = MongobeeChangeSetException.class)
  public void shouldFailOnDuplicatedChangeSets() throws MongobeeChangeSetException {
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new PackageScannedChangeService(scanPackage);
    service.fetchChangeSets(ChangeLogWithDuplicate.class);
  }

}