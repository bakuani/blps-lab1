package ru.urasha.callmeani.blps.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class XmlUserDetailsService implements UserDetailsService {

    private final ResourceLoader resourceLoader;
    private final String xmlPath;
    private final Map<String, XmlUserRecord> users = new ConcurrentHashMap<>();

    public XmlUserDetailsService(
        ResourceLoader resourceLoader,
        @Value("${security.users.xml-path}") String xmlPath
    ) {
        this.resourceLoader = resourceLoader;
        this.xmlPath = xmlPath;
    }

    @PostConstruct
    public void loadUsers() {
        try {
            Resource resource = resourceLoader.getResource(xmlPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Users XML not found: " + xmlPath);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            try (InputStream inputStream = resource.getInputStream()) {
                Document document = factory.newDocumentBuilder().parse(inputStream);
                NodeList userNodes = document.getElementsByTagName("user");
                users.clear();

                for (int i = 0; i < userNodes.getLength(); i++) {
                    Element userElement = (Element) userNodes.item(i);
                    String username = requiredAttr(userElement, "username");
                    String passwordHash = requiredAttr(userElement, "passwordHash");
                    Long subscriberId = parseOptionalLong(userElement.getAttribute("subscriberId"));
                    List<String> roles = parseRoles(userElement);
                    Set<GrantedAuthority> authorities = buildAuthorities(roles);
                    users.put(username, new XmlUserRecord(username, passwordHash, subscriberId, authorities));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load users from XML: " + xmlPath, ex);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        XmlUserRecord record = users.get(username);
        if (record == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new AuthenticatedUser(
            record.username(),
            record.passwordHash(),
            record.subscriberId(),
            record.authorities()
        );
    }

    private String requiredAttr(Element element, String attr) {
        String value = element.getAttribute(attr);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required attribute: " + attr);
        }
        return value.trim();
    }

    private Long parseOptionalLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value.trim());
    }

    private List<String> parseRoles(Element userElement) {
        NodeList roleNodes = userElement.getElementsByTagName("role");
        List<String> roles = new ArrayList<>();
        for (int i = 0; i < roleNodes.getLength(); i++) {
            String role = roleNodes.item(i).getTextContent();
            if (role != null && !role.isBlank()) {
                roles.add(role.trim());
            }
        }
        if (roles.isEmpty()) {
            throw new IllegalStateException("User has no roles configured");
        }
        return roles;
    }

    private Set<GrantedAuthority> buildAuthorities(List<String> roles) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Set<String> permissions = RolePrivileges.resolvePermissions(roles);

        for (String permission : permissions) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    private record XmlUserRecord(
        String username,
        String passwordHash,
        Long subscriberId,
        Set<GrantedAuthority> authorities
    ) {
    }
}
