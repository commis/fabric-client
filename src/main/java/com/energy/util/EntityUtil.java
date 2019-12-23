package com.energy.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author Bryan
 */
@Slf4j
public class EntityUtil {

    public static <T> T get(Class<T> clz, Object o) {
        if (clz.isInstance(o)) {
            return clz.cast(o);
        }
        return null;
    }

    private static <T> Class[] getObjectConstructor(Object[] objects) {
        //确定构造函数
        Class[] c2 = new Class[objects.length];
        for (int i = 0; i < objects.length; ++i) {
            if (objects[i] != null) {
                c2[i] = objects[i].getClass();
            } else {
                c2[i] = String.class;
            }
        }
        return c2;
    }

    public static <T> T castEntity(Object[] objects, Class<T> clazz) {
        Class[] c2 = EntityUtil.getObjectConstructor(objects);
        try {
            Constructor<T> constructor = clazz.getConstructor(c2);
            return constructor.newInstance(objects);
        } catch (Exception e) {
            log.error("failed to cast entity", e);
        }
        return null;
    }

    public static <T> List<T> castEntity(List<Object[]> list, Class<T> clazz) {
        List<T> returnList = new ArrayList<T>();
        if (CollectionUtils.isEmpty(list)) {
            return returnList;
        }

        Class[] c2 = EntityUtil.getObjectConstructor(list.get(0));
        try {
            for (Object[] o : list) {
                Constructor<T> constructor = clazz.getConstructor(c2);
                returnList.add(constructor.newInstance(o));
            }
        } catch (Exception e) {
            log.error("failed to cast entity list", e);
        }
        return returnList;
    }
}
