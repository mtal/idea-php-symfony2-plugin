package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigExtensionParser  {

    private Project project;

    private HashMap<String, TwigExtension> functions;
    private HashMap<String, String> filters;

    public TwigExtensionParser(Project project) {
        this.project = project;
    }

    public HashMap<String, TwigExtension> getFunctions() {
        if(filters == null) {
            this.parseElementType(TwigElementType.METHOD);
        }
        return functions;
    }

    public HashMap<String, String> getFilters() {
        if(filters == null) {
            this.parseElementType(TwigElementType.FILTER);
        }
        return filters;
    }

    public enum TwigElementType {
        FILTER, METHOD
    }

    public enum TwigExtensionType {
        FUNCTION_METHOD, FUNCTION_NODE, SIMPLE_FUNCTION
    }

    private void parseElementType(TwigElementType type) {

        // only the interface gaves use all elements; container dont hold all
        PhpIndex phpIndex = PhpIndex.getInstance(this.project);
        ArrayList<String> classNames = new ArrayList<String>();
        for(PhpClass phpClass : phpIndex.getAllSubclasses("\\Twig_ExtensionInterface")) {
            String className = phpClass.getPresentableFQN();
            if(className != null) {
                // signature class names need slash at first
                classNames.add(className.startsWith("\\") ? className : "\\" + className);
            }
        }

        if(type.equals(TwigElementType.FILTER)) {
            this.parseFilters(classNames);
        }

        if(type.equals(TwigElementType.METHOD)) {
            this.parseFunctions(classNames);
        }

    }

    private void parseFilters(ArrayList<String> classNames) {
        this.filters = new HashMap<String, String>();
        for(String phpClassName : classNames) {
            PhpIndex phpIndex = PhpIndex.getInstance(this.project);
            Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature("#M#C" + phpClassName + "." + "getFilters", null, 0);
            for(PhpNamedElement phpNamedElement: phpNamedElementCollections) {
                if(phpNamedElement instanceof Method) {
                    parseFilter(phpNamedElement.getText(), this.filters);
                }
            }
        }
    }

    private void parseFunctions(ArrayList<String> classNames) {
        this.functions = new HashMap<String, TwigExtension>();
        for(String phpClassName : classNames) {

            PhpIndex phpIndex = PhpIndex.getInstance(this.project);
            Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature("#M#C" + phpClassName + "." + "getFunctions", null, 0);
            for(PhpNamedElement phpNamedElement: phpNamedElementCollections) {
                if(phpNamedElement instanceof Method) {
                    parseFunctions((Method) phpNamedElement, this.functions);
                }
            }
        }
    }

    protected HashMap<String, TwigExtension> parseFunctions(Method method, HashMap<String, TwigExtension> filters) {

        String text = method.getText();

        Matcher simpleFunction = Pattern.compile("[\\\\]*(Twig_SimpleFunction)[\\s+]*\\(['\"](.*?)['\"][\\s+]*,(.*?)[,|)]").matcher(text);
        while(simpleFunction.find()){
            if(!simpleFunction.group(2).contains("*")) {
                filters.put(simpleFunction.group(2),  new TwigExtension(TwigExtensionType.SIMPLE_FUNCTION, "#F" + PsiElementUtils.trimQuote(simpleFunction.group(3).trim())));
            }
        }

        Matcher filterFunction = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Function_Method)\\((.*?),(.*?)[,|)]").matcher(text);
        while(filterFunction.find()){
            if(!filterFunction.group(1).contains("*")) {
                String type = filterFunction.group(2);

                String signature = null;
                if(filterFunction.group(3).trim().equals("$this")) {
                    PhpClass phpClass = method.getContainingClass();
                    if(phpClass != null) {
                        signature = "#M#C\\" + phpClass.getPresentableFQN() + "." + PsiElementUtils.trimQuote(filterFunction.group(4).trim());
                    }
                }

                filters.put(filterFunction.group(1), new TwigExtension(TwigExtensionType.FUNCTION_METHOD, signature));
            }
        }

        Matcher filterFunctionNode = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Function_Node)\\((.*?),").matcher(text);
        while(filterFunctionNode.find()){
            if(!filterFunctionNode.group(1).contains("*")) {
                filters.put(filterFunctionNode.group(1), new TwigExtension(TwigExtensionType.FUNCTION_NODE, "#M#C\\" + PsiElementUtils.trimQuote(filterFunctionNode.group(3).trim()) + ".compile"));
            }
        }

        return filters;

    }

    protected HashMap<String, String> parseFilter(String text, HashMap<String, String> filters) {

        Matcher simpleFilter = Pattern.compile("[\\\\]*(Twig_SimpleFilter)[\\s+]*\\(['\"](.*?)['\"][\\s+]*").matcher(text);
        while(simpleFilter.find()){
            if(!simpleFilter.group(2).contains("*")) {
                filters.put(simpleFilter.group(2), simpleFilter.group(1));
            }
        }


        Matcher filterFunction = Pattern.compile("['\"](.*?)['\"][\\s+]*=>[\\s+]*new[\\s+]*[\\\\]*(Twig_Filter_Function)").matcher(text);
        while(filterFunction.find()){
            if(!filterFunction.group(1).contains("*")) {
                filters.put(filterFunction.group(1), filterFunction.group(2));
            }
        }

        return filters;

    }

    public static Icon getIcon(TwigExtensionType twigExtensionType) {

        if(twigExtensionType == TwigExtensionType.FUNCTION_NODE) {
            return PhpIcons.CLASS_INITIALIZER;
        }

        if(twigExtensionType == TwigExtensionType.SIMPLE_FUNCTION) {
            return PhpIcons.FUNCTION;
        }

        if(twigExtensionType == TwigExtensionType.FUNCTION_METHOD) {
            return PhpIcons.METHOD_ICON;
        }

        return PhpIcons.WEB_ICON;
    }

}
