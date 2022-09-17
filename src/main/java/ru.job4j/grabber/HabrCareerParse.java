package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final int PAGE_AMOUNT = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>();
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = Objects.requireNonNull(titleElement).child(0);
                String vacancyName = titleElement.text();
                String vacancyLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                LocalDateTime date = dateTimeParser.parse(
                        Objects.requireNonNull(row.select(".vacancy-card__date").first()).child(0)
                                .attr("datetime")
                );
                String description = retrieveDescription(vacancyLink);
                posts.add(new Post(vacancyName, vacancyLink, description, date));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return posts;
    }

    private static String getPageLink(int pageNumber) {
        return String.format("%s?page=%s", PAGE_LINK, pageNumber);
    }

    private static String retrieveDescription(String link) {
        String description = "";
        try {
            Connection connection = Jsoup.connect(link);
            Document document = connection.get();
            description = Objects.requireNonNull(document.select(".collapsible-description__content").first())
                    .text();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return description;
    }

    public static void main(String[] args) {
        Parse parser = new HabrCareerParse(new HabrCareerDateTimeParser());
        for (int pageNumber = 1; pageNumber <= PAGE_AMOUNT; pageNumber++) {
            parser.list(getPageLink(pageNumber)).forEach(System.out::println);
        }
    }
}

