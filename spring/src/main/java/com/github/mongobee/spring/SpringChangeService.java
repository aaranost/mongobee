package com.github.mongobee.spring;

import java.util.List;

import com.github.mongobee.changeset.ChangeEntry;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.github.mongobee.utils.ChangeLogComparator;
import com.github.mongobee.utils.ChangeService;
import com.github.mongobee.utils.ChangeSetComparator;
import com.github.mongobee.utils.ListUtils;
import org.reflections.Reflections;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Arrays.asList;

/**
 * Utilities to deal with reflections and annotations
 *
 * @author lstolowski
 * @since 27/07/2014
 */
public class SpringChangeService implements ChangeService {
    private static final String DEFAULT_PROFILE = "default";

    private final String changeLogsBasePackage;
    private final List<String> activeProfiles;

    public SpringChangeService(String changeLogsBasePackage) {
        this(changeLogsBasePackage, null);
    }

    public SpringChangeService(String changeLogsBasePackage, Environment environment) {
        this.changeLogsBasePackage = changeLogsBasePackage;

        if (environment != null && environment.getActiveProfiles() != null && environment.getActiveProfiles().length> 0) {
            this.activeProfiles = asList(environment.getActiveProfiles());
        } else {
            this.activeProfiles = asList(DEFAULT_PROFILE);
        }
    }

    public List<Class<?>> fetchChangeLogs(){
        Reflections reflections = new Reflections(changeLogsBasePackage);
        Set<Class<?>> changeLogs = reflections.getTypesAnnotatedWith(ChangeLog.class); // TODO remove dependency, do own method
        List<Class<?>> filteredChangeLogs = (List<Class<?>>) filterByActiveProfiles(changeLogs);

        Collections.sort(filteredChangeLogs, new ChangeLogComparator());

        return filteredChangeLogs;
    }

    public List<Method> fetchChangeSets(final Class<?> type) {
        final List<Method> changeSets = filterChangeSetAnnotation(asList(type.getDeclaredMethods()));
        final List<Method> filteredChangeSets = (List<Method>) filterByActiveProfiles(changeSets);

        Collections.sort(filteredChangeSets, new ChangeSetComparator());

        return filteredChangeSets;
    }

    public boolean isRunAlwaysChangeSet(Method changesetMethod){
        if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
            ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);
            return annotation.runAlways();
        } else {
            return false;
        }
    }

    public ChangeEntry createChangeEntry(Method changesetMethod){
        if (changesetMethod.isAnnotationPresent(ChangeSet.class)){
            ChangeSet annotation = changesetMethod.getAnnotation(ChangeSet.class);

            return new ChangeEntry(
                    annotation.id(),
                    annotation.author(),
                    new Date(),
                    changesetMethod.getDeclaringClass().getName(),
                    changesetMethod.getName());
        } else {
            return null;
        }
    }

    private boolean matchesActiveSpringProfile(AnnotatedElement element) {
        if (element.isAnnotationPresent(Profile.class)) {
            Profile profiles = element.getAnnotation(Profile.class);
            List<String> values = asList(profiles.value());
            return ListUtils.intersection(activeProfiles, values).size() > 0 ? true : false;

        } else {
            return true; // no-profiled changeset always matches
        }
    }

    private List<?> filterByActiveProfiles(Collection<? extends AnnotatedElement> annotated) {
        List<AnnotatedElement> filtered = new ArrayList<>();
        for (AnnotatedElement element : annotated) {
            if (matchesActiveSpringProfile(element)){
                filtered.add( element);
            }
        }
        return filtered;
    }

    private List<Method> filterChangeSetAnnotation(List<Method> allMethods) {
        final List<Method> changesetMethods = new ArrayList<>();
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(ChangeSet.class)) {
                changesetMethods.add(method);
            }
        }
        return changesetMethods;
    }

}
