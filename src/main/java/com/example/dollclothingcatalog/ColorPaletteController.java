package com.example.dollclothingcatalog;

import static com.example.dollclothingcatalog.CatalogModels.ColorPalette;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ColorPaletteController {
	private final ColorMagicClient colorMagic;
	private final JapaneseKeywordTranslator translator;
	private final CatalogService catalog;

	public ColorPaletteController(ColorMagicClient colorMagic,
			JapaneseKeywordTranslator translator, CatalogService catalog) {
		this.colorMagic = colorMagic;
		this.translator = translator;
		this.catalog = catalog;
	}

	@GetMapping("/color-palettes")
	public ResponseEntity<PaletteSearchResponse> search(@RequestParam String q) {
		if (q == null || q.isBlank() || q.length() > 100) {
			return ResponseEntity.badRequest().body(new PaletteSearchResponse(
					List.of(), "検索キーワードは1〜100文字で入力してください。", q, null, false));
		}
		try {
			var translation = translator.translateToEnglish(q);
			List<ColorPalette> palettes = colorMagic.search(translation.translatedQuery());
			String message = palettes.isEmpty() ? "条件に合う配色が見つかりませんでした。" : null;
			return ResponseEntity.ok(new PaletteSearchResponse(palettes, message,
					translation.originalQuery(), translation.translatedQuery(),
					translation.translated()));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(new PaletteSearchResponse(List.of(),
					exception.getMessage(), q, null, false));
		} catch (JapaneseKeywordTranslator.TranslationException exception) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
					new PaletteSearchResponse(List.of(), exception.getMessage(), q, null, false));
		} catch (ColorMagicClient.ColorMagicException exception) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
					new PaletteSearchResponse(List.of(), exception.getMessage(), q, null, false));
		}
	}

	@PostMapping("/clothes/{itemId}/palettes")
	public ResponseEntity<?> save(@PathVariable long itemId,
			@RequestBody ColorPalette palette) {
		try {
			CatalogService.SavedPaletteSaveResult saved = catalog.saveColorPalette(itemId, palette);
			return ResponseEntity.status(saved.alreadySaved() ? HttpStatus.OK : HttpStatus.CREATED)
					.body(new SavedPaletteResponse(saved.palette(), saved.alreadySaved()));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
		} catch (ResponseStatusException exception) {
			throw exception;
		}
	}

	@DeleteMapping("/clothes/{itemId}/palettes/{savedPaletteId}")
	public ResponseEntity<Void> delete(@PathVariable long itemId,
			@PathVariable long savedPaletteId) {
		if (!catalog.deleteColorPalette(itemId, savedPaletteId)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	public record PaletteSearchResponse(List<ColorPalette> results, String message,
			String originalQuery, String translatedQuery, boolean translated) {
	}

	public record SavedPaletteResponse(CatalogModels.SavedColorPalette palette,
			boolean alreadySaved) {
	}

	public record ErrorResponse(String message) {
	}
}
