package com.ecommerce.ecommerce.service;

import com.ecommerce.ecommerce.annotations.Auditable;
import com.ecommerce.ecommerce.dto.ProductDto;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public ProductDto createProduct(Product product) {
        return modelMapper.map(productRepository.save(product), ProductDto.class);
    }

    @Override
    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id)
                .map(product -> modelMapper.map(product, ProductDto.class));
    }

    @Override
    public Optional<ProductDto> getProductByName(String name) {
        return productRepository.findByName(name)
                .map(product -> modelMapper.map(product, ProductDto.class));
    }

    @Override
    public ProductDto updateProduct(Product product, Product request) {

        product.setName(!Objects.isNull(request.getName()) ? request.getName() : product.getName());
        product.setDescription(!Objects.isNull(request.getDescription()) ? request.getDescription() : product.getDescription());
        product.setPrice(!Objects.isNull(request.getPrice()) ? request.getPrice() : product.getPrice());
        product.setQuantity(!Objects.isNull(request.getQuantity()) ? request.getQuantity() : product.getQuantity());
        product.setImageUrl(!Objects.isNull(request.getImageUrl()) ? request.getImageUrl() : product.getImageUrl());

        productRepository.save(product);
        return modelMapper.map(product, ProductDto.class);
    }

    @Override
    public List<ProductDto> getAllProduct() {
        return productRepository.findAll().stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

}

